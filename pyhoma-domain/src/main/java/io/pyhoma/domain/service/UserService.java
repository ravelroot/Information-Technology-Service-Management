package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.GroupMember;
import io.pyhoma.domain.model.core.InterfaceType;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.Profile;
import io.pyhoma.domain.model.core.ProfileAssignment;
import io.pyhoma.domain.model.core.ProfileRight;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.PasswordHasher;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class UserService extends AbstractService {

    public UserService(Services services) {
        super(services);
    }

    public PageResult<User> search(String text, Boolean active, Long groupId, int page, int size) {
        require(Module.USER, Right.READ);
        Filter f = Filter.and(
                Filter.search(text, "login", "fullName", "email", "jobTitle", "phone"),
                active == null ? Filter.ALL : Filter.eq("active", active),
                groupId == null ? Filter.ALL : Filter.in("id", services.groups().memberIds(groupId)));
        return repo(User.class).page(Query.where(f).orderBy(Sort.asc("fullName")).page(page, size));
    }

    public User get(Long id) {
        return repo(User.class).getById(id);
    }

    public List<User> activeUsers() {
        return repo(User.class).find(Query.where(Filter.isTrue("active")).orderBy(Sort.asc("fullName")));
    }

    public List<User> technicians() {
        Set<Long> techProfiles = new HashSet<>();
        for (Profile p : repo(Profile.class).find(Filter.eq("interfaceType", InterfaceType.STANDARD))) {
            boolean canWork = repo(ProfileRight.class).find(Filter.and(Filter.eq("profileId", p.getId()),
                            Filter.eq("module", Module.TICKET)))
                    .stream().anyMatch(r -> Right.has(r.getRights(), Right.UPDATE));
            if (canWork) {
                techProfiles.add(p.getId());
            }
        }
        Set<Long> userIds = new HashSet<>();
        for (ProfileAssignment a : repo(ProfileAssignment.class).find(Filter.in("profileId", techProfiles))) {
            userIds.add(a.getUserId());
        }
        return repo(User.class).find(Query.where(Filter.and(Filter.in("id", userIds), Filter.isTrue("active")))
                .orderBy(Sort.asc("fullName")));
    }

    public User create(User user, String password) {
        require(Module.USER, Right.CREATE);
        return createInternal(user, password);
    }

    User createInternal(User user, String password) {
        normalize(user);
        validate(user);
        if (!PasswordHasher.isAcceptable(password, user.getLogin(), user.getFullName())) {
            throw new BusinessException("error.weakPassword", PasswordHasher.MIN_LENGTH);
        }
        return tx(() -> {
            checkUniqueLogin(user);
            user.setPasswordHash(PasswordHasher.hash(password));
            return insert(user);
        });
    }

    public User save(User user) {
        if (user.isNew()) {
            throw new IllegalArgumentException("Use create() for new users");
        }
        boolean self = Objects.equals(user.getId(), currentUserId());
        boolean admin = can(Module.USER, Right.UPDATE);
        if (!self && !admin) {
            require(Module.USER, Right.UPDATE);
        }
        normalize(user);
        validate(user);
        return tx(() -> {
            User current = get(user.getId());
            if (!self) {
                guardTarget(current.getId());
            }
            if (self && !user.isActive()) {
                throw new BusinessException("error.cannotDeactivateSelf");
            }
            User target = user;
            if (self && !admin) {
                target = current;
                target.setFullName(user.getFullName());
                target.setEmail(user.getEmail());
                target.setPhone(user.getPhone());
                target.setLanguage(user.getLanguage());
            }
            if (current.isActive() && !target.isActive() && holdsProtectedProfile(current.getId())
                    && activeSuperAdmins(current.getId()) == 0) {
                throw new BusinessException("error.lastSuperAdmin");
            }
            checkUniqueLogin(target);
            target.setPasswordHash(current.getPasswordHash());
            target.setMustChangePassword(current.isMustChangePassword());
            target.setFailedAttempts(current.getFailedAttempts());
            target.setLockedUntil(current.getLockedUntil());
            target.setLastLoginAt(current.getLastLoginAt());
            return update(target);
        });
    }

    public void resetPassword(Long userId, String newPassword, boolean mustChange) {
        require(Module.USER, Right.UPDATE);
        txRun(() -> {
            User u = get(userId);
            guardTarget(userId);
            if (!PasswordHasher.isAcceptable(newPassword, u.getLogin(), u.getFullName())) {
                throw new BusinessException("error.weakPassword", PasswordHasher.MIN_LENGTH);
            }
            u.setPasswordHash(PasswordHasher.hash(newPassword));
            u.setMustChangePassword(mustChange || !Objects.equals(userId, currentUserId()));
            u.setFailedAttempts(0);
            u.setLockedUntil(null);
            update(u);
        });
    }

    public void unlock(Long userId) {
        require(Module.USER, Right.UPDATE);
        txRun(() -> {
            User u = get(userId);
            guardTarget(userId);
            u.setFailedAttempts(0);
            u.setLockedUntil(null);
            saveQuietly(u);
        });
    }

    public List<ProfileAssignment> assignments(Long userId) {
        return repo(ProfileAssignment.class).find(Query.where(Filter.eq("userId", userId)).orderBy(Sort.asc("id")));
    }

    public ProfileAssignment addAssignment(Long userId, Long profileId, Long orgUnitId, boolean includeChildren) {
        require(Module.USER, Right.UPDATE);
        return tx(() -> {
            if (Objects.equals(userId, currentUserId()) && !holdsProtectedProfile(currentUserId())) {
                throw new BusinessException("error.ownAccessChange");
            }
            guardTarget(userId);
            guardGrant(profileId);
            if (!security().current().isSystem() && !holdsProtectedProfile(currentUserId())
                    && !security().current().seesAllOrgUnits()
                    && !services.auth().accessibleOrgUnits().stream().anyMatch(u -> Objects.equals(u.getId(), orgUnitId))) {
                throw new BusinessException("error.orgUnitNotAllowed");
            }
            return addAssignmentInternal(userId, profileId, orgUnitId, includeChildren);
        });
    }

    ProfileAssignment addAssignmentInternal(Long userId, Long profileId, Long orgUnitId, boolean includeChildren) {
        return tx(() -> {
            boolean exists = repo(ProfileAssignment.class).exists(Filter.and(Filter.eq("userId", userId),
                    Filter.eq("profileId", profileId), Filter.eq("orgUnitId", orgUnitId)));
            if (exists) {
                throw new BusinessException("error.duplicateAssignment");
            }
            ProfileAssignment a = new ProfileAssignment();
            a.setUserId(userId);
            a.setProfileId(profileId);
            a.setOrgUnitId(orgUnitId);
            a.setIncludeChildren(includeChildren);
            return insert(a);
        });
    }

    public void removeAssignment(Long assignmentId) {
        require(Module.USER, Right.UPDATE);
        txRun(() -> {
            ProfileAssignment a = repo(ProfileAssignment.class).getById(assignmentId);
            if (Objects.equals(a.getUserId(), currentUserId())
                    && repo(ProfileAssignment.class).count(Filter.eq("userId", a.getUserId())) <= 1) {
                throw new BusinessException("error.lastOwnAssignment");
            }
            if (Objects.equals(a.getUserId(), currentUserId()) && !holdsProtectedProfile(currentUserId())) {
                throw new BusinessException("error.ownAccessChange");
            }
            guardTarget(a.getUserId());
            Profile p = repo(Profile.class).getById(a.getProfileId());
            if (p.isProtectedProfile()) {
                long others = repo(ProfileAssignment.class).count(Filter.and(Filter.eq("userId", a.getUserId()),
                        Filter.in("profileId", protectedProfileIds())));
                if (others <= 1 && activeSuperAdmins(a.getUserId()) == 0) {
                    throw new BusinessException("error.lastSuperAdmin");
                }
            }
            purge(a);
        });
    }

    boolean holdsProtectedProfile(Long userId) {
        if (userId == null) {
            return true;
        }
        Set<Long> ids = protectedProfileIds();
        return !ids.isEmpty() && repo(ProfileAssignment.class).exists(Filter.and(Filter.eq("userId", userId), Filter.in("profileId", ids)));
    }

    private Set<Long> protectedProfileIds() {
        Set<Long> ids = new HashSet<>();
        repo(Profile.class).find(Filter.isTrue("protectedProfile")).forEach(p -> ids.add(p.getId()));
        return ids;
    }

    private long activeSuperAdmins(Long excludingUserId) {
        Set<Long> ids = protectedProfileIds();
        if (ids.isEmpty()) {
            return 1;
        }
        Set<Long> users = new HashSet<>();
        repo(ProfileAssignment.class).find(Filter.in("profileId", ids)).forEach(a -> users.add(a.getUserId()));
        users.remove(excludingUserId);
        if (users.isEmpty()) {
            return 0;
        }
        return repo(User.class).count(Filter.and(Filter.in("id", users), Filter.isTrue("active")));
    }

    private void guardTarget(Long targetUserId) {
        if (security().current().isSystem() || Objects.equals(targetUserId, currentUserId())) {
            return;
        }
        if (holdsProtectedProfile(targetUserId) && !holdsProtectedProfile(currentUserId())) {
            throw new BusinessException("error.privilegeEscalation");
        }
    }

    private void guardGrant(Long profileId) {
        if (security().current().isSystem() || holdsProtectedProfile(currentUserId())) {
            return;
        }
        Profile p = repo(Profile.class).getById(profileId);
        if (p.isProtectedProfile()) {
            throw new BusinessException("error.privilegeEscalation");
        }
        services.profiles().rights(profileId).forEach((module, mask) -> {
            if ((mask & ~security().current().rights(module)) != 0) {
                throw new BusinessException("error.privilegeEscalation");
            }
        });
    }

    public List<Long> groupIds(Long userId) {
        return repo(GroupMember.class).find(Filter.eq("userId", userId)).stream().map(GroupMember::getGroupId).toList();
    }

    private void normalize(User user) {
        user.setLogin(Texts.clean(user.getLogin()) == null ? null : user.getLogin().trim().toLowerCase(Locale.ROOT));
        user.setFullName(Texts.clean(user.getFullName()));
        user.setEmail(Texts.clean(user.getEmail()));
    }

    private void validate(User user) {
        Validator.create()
                .required("login", user.getLogin())
                .maxLength("login", user.getLogin(), 80)
                .check(user.getLogin() == null || user.getLogin().matches("[a-z0-9._@-]+"), "login", "validation.login")
                .required("fullName", user.getFullName())
                .maxLength("fullName", user.getFullName(), 150)
                .email("email", user.getEmail())
                .throwIfInvalid();
    }

    private void checkUniqueLogin(User user) {
        boolean taken = repo(User.class).find(Filter.eq("login", user.getLogin())).stream()
                .anyMatch(u -> !Objects.equals(u.getId(), user.getId()));
        if (taken) {
            throw new BusinessException("error.loginTaken", user.getLogin());
        }
    }
}
