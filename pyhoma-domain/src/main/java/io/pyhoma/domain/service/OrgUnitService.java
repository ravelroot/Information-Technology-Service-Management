package io.pyhoma.domain.service;

import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.OrgUnit;
import io.pyhoma.domain.model.core.ProfileAssignment;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OrgUnitService extends AbstractService {

    public OrgUnitService(Services services) {
        super(services);
    }

    public List<OrgUnit> all() {
        return repo(OrgUnit.class).find(Query.all().orderBy(Sort.asc("completeName")));
    }

    public OrgUnit get(Long id) {
        return repo(OrgUnit.class).getById(id);
    }

    public OrgUnit root() {
        return repo(OrgUnit.class).findFirst(Query.where(Filter.isNull("parentId")).orderBy(Sort.asc("id")))
                .orElseThrow(() -> new BusinessException("error.noRootOrgUnit"));
    }

    public Set<Long> subtree(Long id) {
        return subtree(id, all());
    }

    static Set<Long> subtree(Long id, List<OrgUnit> units) {
        Map<Long, List<OrgUnit>> children = units.stream().filter(u -> u.getParentId() != null)
                .collect(Collectors.groupingBy(OrgUnit::getParentId));
        Set<Long> result = new LinkedHashSet<>();
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(id);
        while (!stack.isEmpty()) {
            Long current = stack.pop();
            if (result.add(current)) {
                for (OrgUnit c : children.getOrDefault(current, List.of())) {
                    stack.push(c.getId());
                }
            }
        }
        return result;
    }

    public List<OrgUnit> ancestors(Long id) {
        Map<Long, OrgUnit> byId = all().stream().collect(Collectors.toMap(OrgUnit::getId, Function.identity()));
        List<OrgUnit> chain = new ArrayList<>();
        OrgUnit current = byId.get(id);
        while (current != null && current.getParentId() != null) {
            current = byId.get(current.getParentId());
            if (current != null) {
                chain.add(0, current);
            }
        }
        return chain;
    }

    public OrgUnit save(OrgUnit unit) {
        require(Module.ORG_UNIT, unit.isNew() ? Right.CREATE : Right.UPDATE);
        unit.setName(Texts.clean(unit.getName()));
        Validator.create().required("name", unit.getName()).maxLength("name", unit.getName(), 150)
                .email("email", unit.getEmail()).throwIfInvalid();
        return tx(() -> {
            OrgUnit parent = null;
            if (unit.getParentId() != null) {
                parent = get(unit.getParentId());
                if (!unit.isNew() && subtree(unit.getId()).contains(parent.getId())) {
                    throw new BusinessException("error.treeCycle");
                }
            } else if (unit.isNew() && repo(OrgUnit.class).count(Filter.isNull("parentId")) > 0) {
                throw new BusinessException("error.singleRoot");
            }
            String oldName = unit.isNew() ? null : get(unit.getId()).getCompleteName();
            unit.setDepth(parent == null ? 0 : parent.getDepth() + 1);
            unit.setCompleteName(parent == null ? unit.getName() : parent.getCompleteName() + " > " + unit.getName());
            OrgUnit saved = persist(unit);
            if (oldName != null && !oldName.equals(saved.getCompleteName())) {
                refreshChildren(saved);
            }
            return saved;
        });
    }

    private void refreshChildren(OrgUnit parent) {
        for (OrgUnit child : repo(OrgUnit.class).find(Filter.eq("parentId", parent.getId()))) {
            child.setDepth(parent.getDepth() + 1);
            child.setCompleteName(parent.getCompleteName() + " > " + child.getName());
            OrgUnit saved = saveQuietly(child);
            refreshChildren(saved);
        }
    }

    public void delete(Long id) {
        require(Module.ORG_UNIT, Right.DELETE);
        txRun(() -> {
            OrgUnit unit = get(id);
            if (unit.getParentId() == null) {
                throw new BusinessException("error.cannotDeleteRoot");
            }
            if (repo(OrgUnit.class).exists(Filter.eq("parentId", id))
                    || repo(Asset.class).exists(Filter.eq("orgUnitId", id))
                    || repo(Ticket.class).exists(Filter.eq("orgUnitId", id))
                    || repo(ProfileAssignment.class).exists(Filter.eq("orgUnitId", id))) {
                throw new BusinessException("error.inUse", unit.getName());
            }
            purge(unit);
        });
    }

    public List<Long> selfAndAncestors(Long id) {
        List<Long> chain = new ArrayList<>();
        chain.add(id);
        ancestors(id).forEach(a -> chain.add(1, a.getId()));
        return chain.stream().filter(Objects::nonNull).toList();
    }
}
