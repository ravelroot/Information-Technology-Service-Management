package io.pyhoma.desktop.ui;

import io.pyhoma.domain.model.asset.Supplier;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.OrgUnit;
import io.pyhoma.domain.model.core.Profile;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.core.UserGroup;
import io.pyhoma.domain.model.itil.Sla;
import io.pyhoma.domain.model.project.Project;
import io.pyhoma.domain.service.Services;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LookupCache {

    private final Services services;
    private final Map<Class<?>, Map<Long, String>> names = new ConcurrentHashMap<>();
    private final Map<DropdownKind, Map<Long, String>> dropdowns = new EnumMap<>(DropdownKind.class);
    private final Map<DropdownKind, Map<Long, String>> colors = new EnumMap<>(DropdownKind.class);
    private volatile List<User> technicians;

    public LookupCache(Services services) {
        this.services = services;
    }

    public synchronized void invalidate() {
        names.clear();
        dropdowns.clear();
        colors.clear();
        technicians = null;
    }

    public void refreshAsync(Runnable after) {
        invalidate();
        Async.run(() -> {
            warmUp();
            return Boolean.TRUE;
        }, ok -> {
            if (after != null) {
                after.run();
            }
        });
    }

    public void warmUp() {
        users();
        groups();
        orgUnits();
        suppliers();
        for (DropdownKind k : DropdownKind.values()) {
            dropdown(k);
        }
        technicians();
    }

    private Map<Long, String> load(Class<?> type) {
        return names.computeIfAbsent(type, t -> {
            if (t == User.class) {
                return services.lookups().names(User.class);
            } else if (t == UserGroup.class) {
                return services.lookups().names(UserGroup.class);
            } else if (t == OrgUnit.class) {
                return services.lookups().names(OrgUnit.class);
            } else if (t == Supplier.class) {
                return services.lookups().names(Supplier.class);
            } else if (t == Profile.class) {
                return services.lookups().names(Profile.class);
            } else if (t == Sla.class) {
                return services.lookups().names(Sla.class);
            } else if (t == Project.class) {
                return services.lookups().names(Project.class);
            }
            throw new IllegalArgumentException(t.getName());
        });
    }

    public Map<Long, String> users() {
        return load(User.class);
    }

    public Map<Long, String> groups() {
        return load(UserGroup.class);
    }

    public Map<Long, String> orgUnits() {
        return load(OrgUnit.class);
    }

    public Map<Long, String> suppliers() {
        return load(Supplier.class);
    }

    public Map<Long, String> profiles() {
        return load(Profile.class);
    }

    public Map<Long, String> slas() {
        return load(Sla.class);
    }

    public Map<Long, String> projects() {
        return load(Project.class);
    }

    public synchronized Map<Long, String> dropdown(DropdownKind kind) {
        return dropdowns.computeIfAbsent(kind, k -> services.lookups().dropdownNames(k));
    }

    public synchronized Map<Long, String> colors(DropdownKind kind) {
        return colors.computeIfAbsent(kind, k -> services.lookups().dropdownColors(k));
    }

    public List<User> technicians() {
        List<User> t = technicians;
        if (t == null) {
            t = services.users().technicians();
            technicians = t;
        }
        return t;
    }

    public String user(Long id) {
        return name(users(), id);
    }

    public String group(Long id) {
        return name(groups(), id);
    }

    public String orgUnit(Long id) {
        return name(orgUnits(), id);
    }

    public String supplier(Long id) {
        return name(suppliers(), id);
    }

    public String dd(DropdownKind kind, Long id) {
        return name(dropdown(kind), id);
    }

    public static String name(Map<Long, String> map, Long id) {
        if (id == null) {
            return "—";
        }
        return map.getOrDefault(id, "#" + id);
    }
}
