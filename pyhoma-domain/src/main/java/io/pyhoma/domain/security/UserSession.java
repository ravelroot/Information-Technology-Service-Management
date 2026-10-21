package io.pyhoma.domain.security;

import io.pyhoma.domain.model.core.InterfaceType;
import io.pyhoma.domain.model.core.Module;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public record UserSession(
        Long userId,
        String login,
        String fullName,
        Long activeOrgUnitId,
        boolean includeChildren,
        Set<Long> visibleOrgUnits,
        Set<Long> groupIds,
        Map<Module, Integer> rights,
        InterfaceType interfaceType,
        String language) {

    public UserSession {
        visibleOrgUnits = visibleOrgUnits == null ? null : Set.copyOf(visibleOrgUnits);
        groupIds = groupIds == null ? Set.of() : Set.copyOf(groupIds);
        rights = Collections.unmodifiableMap(rights.isEmpty() ? new EnumMap<>(Module.class) : new EnumMap<>(rights));
    }

    public static UserSession system() {
        Map<Module, Integer> all = new EnumMap<>(Module.class);
        for (Module m : Module.values()) {
            all.put(m, Right.ALL);
        }
        return new UserSession(null, "system", "Pyhoma", null, true, null, Set.of(), all, InterfaceType.STANDARD, null);
    }

    public boolean isSystem() {
        return userId == null;
    }

    public int rights(Module module) {
        return rights.getOrDefault(module, 0);
    }

    public boolean can(Module module, int right) {
        return Right.has(rights(module), right);
    }

    public boolean seesAllOrgUnits() {
        return visibleOrgUnits == null;
    }

    public boolean canSeeOrgUnit(Long orgUnitId) {
        return visibleOrgUnits == null || (orgUnitId != null && visibleOrgUnits.contains(orgUnitId));
    }

    public boolean isSelfService() {
        return interfaceType == InterfaceType.SELF_SERVICE;
    }
}
