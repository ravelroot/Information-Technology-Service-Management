package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.Profile;
import io.pyhoma.domain.model.core.ProfileAssignment;
import io.pyhoma.domain.model.core.ProfileRight;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProfileService extends AbstractService {

    public ProfileService(Services services) {
        super(services);
    }

    public List<Profile> all() {
        return repo(Profile.class).find(Query.all().orderBy(Sort.asc("name")));
    }

    public Profile get(Long id) {
        return repo(Profile.class).getById(id);
    }

    public Map<Module, Integer> rights(Long profileId) {
        Map<Module, Integer> result = new EnumMap<>(Module.class);
        for (ProfileRight r : repo(ProfileRight.class).find(Filter.eq("profileId", profileId))) {
            result.put(r.getModule(), r.getRights());
        }
        return result;
    }

    public Profile save(Profile profile, Map<Module, Integer> rights) {
        require(Module.PROFILE, profile.isNew() ? Right.CREATE : Right.UPDATE);
        return tx(() -> {
            boolean superAdmin = services.users().holdsProtectedProfile(currentUserId());
            if (profile.isNew()) {
                profile.setProtectedProfile(false);
            } else {
                Profile existing = get(profile.getId());
                profile.setProtectedProfile(existing.isProtectedProfile());
                if (existing.isProtectedProfile() && !superAdmin) {
                    throw new BusinessException("error.protectedProfile", existing.getName());
                }
                if (existing.isProtectedProfile() && rights != null && !normalized(rights).equals(normalized(rights(existing.getId())))) {
                    throw new BusinessException("error.protectedProfile", existing.getName());
                }
            }
            if (!superAdmin && rights != null) {
                rights.forEach((module, mask) -> {
                    if ((mask & module.allowedRights() & ~security().current().rights(module)) != 0) {
                        throw new BusinessException("error.privilegeEscalation");
                    }
                });
            }
            return saveInternal(profile, rights);
        });
    }

    private static Map<Module, Integer> normalized(Map<Module, Integer> rights) {
        Map<Module, Integer> m = new EnumMap<>(Module.class);
        for (Module module : Module.values()) {
            int mask = rights.getOrDefault(module, 0) & module.allowedRights();
            if (mask != 0) {
                m.put(module, mask);
            }
        }
        return m;
    }

    Profile saveInternal(Profile profile, Map<Module, Integer> rights) {
        profile.setName(Texts.clean(profile.getName()));
        Validator.create().required("name", profile.getName()).required("interfaceType", profile.getInterfaceType())
                .throwIfInvalid();
        return tx(() -> {
            boolean duplicate = repo(Profile.class).find(Filter.ALL).stream()
                    .anyMatch(p -> !Objects.equals(p.getId(), profile.getId())
                            && Texts.fold(p.getName()).equals(Texts.fold(profile.getName())));
            if (duplicate) {
                throw new BusinessException("error.duplicateName", profile.getName());
            }
            if (profile.isDefaultProfile()) {
                for (Profile other : repo(Profile.class).find(Filter.isTrue("defaultProfile"))) {
                    if (!Objects.equals(other.getId(), profile.getId())) {
                        other.setDefaultProfile(false);
                        update(other);
                    }
                }
            }
            Profile saved = persist(profile);
            if (rights != null) {
                writeRights(saved.getId(), rights);
            }
            return saved;
        });
    }

    private void writeRights(Long profileId, Map<Module, Integer> rights) {
        Map<Module, ProfileRight> existing = new EnumMap<>(Module.class);
        for (ProfileRight r : repo(ProfileRight.class).find(Filter.eq("profileId", profileId))) {
            existing.put(r.getModule(), r);
        }
        for (Module m : Module.values()) {
            int mask = rights.getOrDefault(m, 0) & m.allowedRights();
            ProfileRight r = existing.get(m);
            if (r == null) {
                r = new ProfileRight();
                r.setProfileId(profileId);
                r.setModule(m);
            } else if (r.getRights() == mask) {
                continue;
            }
            r.setRights(mask);
            persist(r);
        }
    }

    public Profile duplicate(Long profileId, String newName) {
        require(Module.PROFILE, Right.CREATE);
        Profile source = get(profileId);
        Profile copy = new Profile();
        copy.setName(newName);
        copy.setInterfaceType(source.getInterfaceType());
        copy.setNotes(source.getNotes());
        Map<Module, Integer> copied = new EnumMap<>(Module.class);
        copied.putAll(rights(profileId));
        return save(copy, copied);
    }

    public void delete(Long profileId) {
        require(Module.PROFILE, Right.DELETE);
        txRun(() -> {
            Profile p = get(profileId);
            if (p.isProtectedProfile()) {
                throw new BusinessException("error.protectedProfile", p.getName());
            }
            if (repo(ProfileAssignment.class).exists(Filter.eq("profileId", profileId))) {
                throw new BusinessException("error.inUse", p.getName());
            }
            repo(ProfileRight.class).deleteWhere(Filter.eq("profileId", profileId));
            purge(p);
        });
    }

    public long assignmentCount(Long profileId) {
        return repo(ProfileAssignment.class).count(Filter.eq("profileId", profileId));
    }
}
