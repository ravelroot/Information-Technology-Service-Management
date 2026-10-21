package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.AuditAction;
import io.pyhoma.domain.model.core.InterfaceType;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.OrgUnit;
import io.pyhoma.domain.model.core.Profile;
import io.pyhoma.domain.model.core.ProfileAssignment;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.security.PasswordHasher;
import io.pyhoma.domain.security.UserSession;
import io.pyhoma.domain.support.BusinessException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AuthService extends AbstractService {

    public AuthService(Services services) {
        super(services);
    }

    public UserSession login(String login, String password) {
        String normalized = login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 100) {
            normalized = normalized.substring(0, 100);
        }
        String key = normalized;
        UserSession session = security().runAsSystem(() -> tx(() -> {
            User user = repo(User.class).findFirst(Filter.eq("login", key)).orElse(null);
            LocalDateTime now = now();
            if (user == null) {
                PasswordHasher.verifyDummy(password);
                services.audit().record("User", null, AuditAction.LOGIN_FAILED, "login", null, key);
                return null;
            }
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
                PasswordHasher.verifyDummy(password);
                services.audit().record(user, AuditAction.LOGIN_FAILED, "lockedUntil", null, null);
                return null;
            }
            if (!PasswordHasher.verify(password, user.getPasswordHash())) {
                int max = Math.max(1, services.settings().getInt(SettingKeys.MAX_FAILED_LOGINS, 5));
                user.setFailedAttempts(user.getFailedAttempts() + 1);
                if (user.getFailedAttempts() >= max) {
                    int minutes = Math.max(1, services.settings().getInt(SettingKeys.LOCK_MINUTES, 15));
                    user.setLockedUntil(now.plusMinutes(minutes));
                    user.setFailedAttempts(0);
                }
                saveQuietly(user);
                services.audit().record(user, AuditAction.LOGIN_FAILED, null, null, null);
                return null;
            }
            if (!user.isActive()) {
                throw new BusinessException("error.accountDisabled");
            }
            if (PasswordHasher.needsRehash(user.getPasswordHash())) {
                user.setPasswordHash(PasswordHasher.hash(password));
            }
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            user.setLastLoginAt(now);
            User saved = saveQuietly(user);
            UserSession s = buildSession(saved, null, true);
            services.audit().record(saved, AuditAction.LOGIN, null, null, null);
            return s;
        }));
        if (session == null) {
            slowDown();
            throw new BusinessException("error.invalidCredentials");
        }
        security().login(session);
        return session;
    }

    private static void slowDown() {
        try {
            Thread.sleep(400 + new java.security.SecureRandom().nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean validateSession() {
        if (!security().isAuthenticated() || security().current().isSystem()) {
            return security().isAuthenticated();
        }
        UserSession current = security().current();
        User user = security().runAsSystem(() -> repo(User.class).findById(current.userId()).orElse(null));
        if (user == null || !user.isActive() || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now()))) {
            security().logout();
            return false;
        }
        try {
            UserSession s = security().runAsSystem(() -> buildSession(user, current.activeOrgUnitId(), current.includeChildren()));
            security().login(s);
            return true;
        } catch (BusinessException e) {
            security().logout();
            return false;
        }
    }

    public void logout() {
        security().logout();
    }

    public boolean mustChangePassword() {
        return security().isAuthenticated() && !security().current().isSystem()
                && repo(User.class).getById(currentUserId()).isMustChangePassword();
    }

    public void changePassword(String currentPassword, String newPassword) {
        txRun(() -> {
            User user = repo(User.class).getById(currentUserId());
            if (!PasswordHasher.verify(currentPassword, user.getPasswordHash())) {
                throw new BusinessException("error.wrongPassword");
            }
            if (!PasswordHasher.isAcceptable(newPassword, user.getLogin(), user.getFullName())) {
                throw new BusinessException("error.weakPassword", PasswordHasher.MIN_LENGTH);
            }
            if (Objects.equals(currentPassword, newPassword)) {
                throw new BusinessException("error.samePassword");
            }
            user.setPasswordHash(PasswordHasher.hash(newPassword));
            user.setMustChangePassword(false);
            update(user);
        });
    }

    public List<OrgUnit> accessibleOrgUnits() {
        UserSession s = security().current();
        if (s.isSystem()) {
            return services.orgUnits().all();
        }
        List<OrgUnit> all = services.orgUnits().all();
        Set<Long> ids = accessibleIds(s.userId(), all);
        return all.stream().filter(u -> ids.contains(u.getId())).toList();
    }

    public UserSession switchOrgUnit(Long orgUnitId, boolean includeChildren) {
        User user = repo(User.class).getById(currentUserId());
        UserSession s = security().runAsSystem(() -> buildSession(user, orgUnitId, includeChildren));
        security().login(s);
        return s;
    }

    public UserSession refreshSession() {
        UserSession current = security().current();
        User user = repo(User.class).getById(current.userId());
        UserSession s = security().runAsSystem(() -> buildSession(user, current.activeOrgUnitId(), current.includeChildren()));
        security().login(s);
        return s;
    }

    UserSession buildSession(User user, Long requestedOrgUnit, boolean includeChildren) {
        List<OrgUnit> units = services.orgUnits().all();
        List<ProfileAssignment> assignments = repo(ProfileAssignment.class).find(Filter.eq("userId", user.getId()));
        if (assignments.isEmpty()) {
            throw new BusinessException("error.noProfile");
        }
        Set<Long> accessible = accessibleIds(user.getId(), units);
        Long active = requestedOrgUnit;
        if (active == null || !accessible.contains(active)) {
            active = user.getDefaultOrgUnitId() != null && accessible.contains(user.getDefaultOrgUnitId())
                    ? user.getDefaultOrgUnitId()
                    : units.stream().filter(u -> accessible.contains(u.getId()))
                    .min(Comparator.comparing(OrgUnit::getDepth)).map(OrgUnit::getId).orElseThrow();
        }
        Map<Module, Integer> rights = new EnumMap<>(Module.class);
        boolean standard = false;
        for (ProfileAssignment a : assignments) {
            boolean applies = Objects.equals(a.getOrgUnitId(), active)
                    || (a.isIncludeChildren() && OrgUnitService.subtree(a.getOrgUnitId(), units).contains(active));
            if (!applies) {
                continue;
            }
            Profile p = repo(Profile.class).getById(a.getProfileId());
            standard |= p.getInterfaceType() == InterfaceType.STANDARD;
            services.profiles().rights(p.getId()).forEach((m, mask) -> rights.merge(m, mask, (x, y) -> x | y));
        }
        Set<Long> visible = new LinkedHashSet<>();
        if (includeChildren) {
            for (Long id : OrgUnitService.subtree(active, units)) {
                if (accessible.contains(id)) {
                    visible.add(id);
                }
            }
        } else {
            visible.add(active);
        }
        Set<Long> groups = new HashSet<>(services.users().groupIds(user.getId()));
        return new UserSession(user.getId(), user.getLogin(), user.getFullName(), active, includeChildren, visible,
                groups, rights, standard ? InterfaceType.STANDARD : InterfaceType.SELF_SERVICE, user.getLanguage());
    }

    private Set<Long> accessibleIds(Long userId, List<OrgUnit> units) {
        Set<Long> ids = new LinkedHashSet<>();
        for (ProfileAssignment a : repo(ProfileAssignment.class).find(Filter.eq("userId", userId))) {
            if (a.isIncludeChildren()) {
                ids.addAll(OrgUnitService.subtree(a.getOrgUnitId(), units));
            } else {
                ids.add(a.getOrgUnitId());
            }
        }
        return ids;
    }

    List<Long> allUserIds() {
        List<Long> ids = new ArrayList<>();
        repo(User.class).find(Filter.ALL).forEach(u -> ids.add(u.getId()));
        return ids;
    }
}
