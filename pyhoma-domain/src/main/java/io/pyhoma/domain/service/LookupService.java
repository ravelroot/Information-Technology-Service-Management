package io.pyhoma.domain.service;

import io.pyhoma.domain.model.BaseEntity;
import io.pyhoma.domain.model.Named;
import io.pyhoma.domain.model.core.DropdownItem;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.OrgUnit;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.Query;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class LookupService extends AbstractService {

    public LookupService(Services services) {
        super(services);
    }

    public <T extends BaseEntity & Named> Map<Long, String> names(Class<T> type) {
        Map<Long, String> names = new HashMap<>();
        for (T row : repo(type).find(Query.all())) {
            names.put(row.getId(), row instanceof OrgUnit o && o.getCompleteName() != null ? o.getCompleteName() : row.displayName());
        }
        return names;
    }

    public <T extends BaseEntity & Named> Map<Long, String> names(Class<T> type, java.util.Collection<Long> ids) {
        Map<Long, String> names = new HashMap<>();
        java.util.List<Long> distinct = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        for (T row : repo(type).findAllById(distinct)) {
            names.put(row.getId(), row.displayName());
        }
        return names;
    }

    public Map<Long, String> dropdownNames(DropdownKind kind) {
        return services.dropdowns().names(kind);
    }

    public Map<Long, String> allDropdownNames() {
        Map<Long, String> names = new HashMap<>();
        for (DropdownItem d : repo(DropdownItem.class).find(Query.all())) {
            names.put(d.getId(), d.getCompleteName() == null ? d.getName() : d.getCompleteName());
        }
        return names;
    }

    public Map<Long, String> dropdownColors(DropdownKind kind) {
        Map<Long, String> colors = new LinkedHashMap<>();
        for (DropdownItem d : repo(DropdownItem.class).find(Filter.eq("kind", kind))) {
            if (d.getColor() != null) {
                colors.put(d.getId(), d.getColor());
            }
        }
        return colors;
    }
}
