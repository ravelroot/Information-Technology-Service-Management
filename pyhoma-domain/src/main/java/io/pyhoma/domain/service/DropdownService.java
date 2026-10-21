package io.pyhoma.domain.service;

import io.pyhoma.domain.model.core.DropdownItem;
import io.pyhoma.domain.model.core.DropdownKind;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DropdownService extends AbstractService {

    public DropdownService(Services services) {
        super(services);
    }

    public List<DropdownItem> list(DropdownKind kind, boolean activeOnly) {
        Filter f = Filter.and(Filter.eq("kind", kind), activeOnly ? Filter.isTrue("active") : Filter.ALL);
        return repo(DropdownItem.class).find(Query.where(f).orderBy(Sort.asc("sortOrder"), Sort.asc("completeName")));
    }

    public Map<Long, String> names(DropdownKind kind) {
        Map<Long, String> names = new LinkedHashMap<>();
        for (DropdownItem item : list(kind, false)) {
            names.put(item.getId(), item.getCompleteName() == null ? item.getName() : item.getCompleteName());
        }
        return names;
    }

    public DropdownItem get(Long id) {
        return repo(DropdownItem.class).getById(id);
    }

    public DropdownItem save(DropdownItem item) {
        require(Module.DROPDOWN, item.isNew() ? Right.CREATE : Right.UPDATE);
        return saveInternal(item);
    }

    DropdownItem saveInternal(DropdownItem item) {
        item.setName(Texts.clean(item.getName()));
        Validator.create().required("kind", item.getKind()).required("name", item.getName())
                .maxLength("name", item.getName(), 150).throwIfInvalid();
        if (!item.getKind().isHierarchical()) {
            item.setParentId(null);
        }
        return tx(() -> {
            DropdownItem parent = null;
            if (item.getParentId() != null) {
                parent = get(item.getParentId());
                if (Objects.equals(parent.getId(), item.getId()) || isDescendant(item.getId(), parent)) {
                    throw new BusinessException("error.treeCycle");
                }
            }
            boolean duplicate = repo(DropdownItem.class).find(Filter.and(Filter.eq("kind", item.getKind()),
                            Filter.eq("parentId", item.getParentId())))
                    .stream().anyMatch(d -> !Objects.equals(d.getId(), item.getId())
                            && Texts.fold(d.getName()).equals(Texts.fold(item.getName())));
            if (duplicate) {
                throw new BusinessException("error.duplicateName", item.getName());
            }
            String old = item.isNew() ? null : get(item.getId()).getCompleteName();
            item.setCompleteName(parent == null ? item.getName() : parent.getCompleteName() + " > " + item.getName());
            DropdownItem saved = persist(item);
            if (old != null && !old.equals(saved.getCompleteName())) {
                refreshChildren(saved);
            }
            return saved;
        });
    }

    private boolean isDescendant(Long ancestorId, DropdownItem candidate) {
        if (ancestorId == null) {
            return false;
        }
        DropdownItem current = candidate;
        while (current != null && current.getParentId() != null) {
            if (ancestorId.equals(current.getParentId())) {
                return true;
            }
            current = repo(DropdownItem.class).findById(current.getParentId()).orElse(null);
        }
        return false;
    }

    private void refreshChildren(DropdownItem parent) {
        for (DropdownItem child : repo(DropdownItem.class).find(Filter.eq("parentId", parent.getId()))) {
            child.setCompleteName(parent.getCompleteName() + " > " + child.getName());
            refreshChildren(saveQuietly(child));
        }
    }

    public void delete(Long id) {
        require(Module.DROPDOWN, Right.DELETE);
        txRun(() -> {
            DropdownItem item = get(id);
            if (item.getKind() == DropdownKind.ITIL_CATEGORY) {
                deleteCategoryTree(item);
                return;
            }
            if (repo(DropdownItem.class).exists(Filter.eq("parentId", id))) {
                throw new BusinessException("error.inUse", item.getName());
            }
            purge(item);
        });
    }

    public List<DropdownItem> subtree(Long id) {
        List<DropdownItem> out = new java.util.ArrayList<>();
        collect(get(id), out);
        return out;
    }

    public long categoryUsage(Long id) {
        List<Long> ids = subtree(id).stream().map(DropdownItem::getId).toList();
        Filter used = Filter.in("categoryId", ids);
        return repo(io.pyhoma.domain.model.itil.Ticket.class).count(used)
                + repo(io.pyhoma.domain.model.itil.Problem.class).count(used)
                + repo(io.pyhoma.domain.model.itil.Change.class).count(used);
    }

    private void collect(DropdownItem node, List<DropdownItem> out) {
        out.add(node);
        for (DropdownItem child : repo(DropdownItem.class).find(Filter.eq("parentId", node.getId()))) {
            collect(child, out);
        }
    }

    private void deleteCategoryTree(DropdownItem root) {
        List<DropdownItem> nodes = new java.util.ArrayList<>();
        collect(root, nodes);
        List<Long> ids = nodes.stream().map(DropdownItem::getId).toList();
        Filter used = Filter.in("categoryId", ids);
        clearCategory(io.pyhoma.domain.model.itil.Ticket.class, used);
        clearCategory(io.pyhoma.domain.model.itil.Problem.class, used);
        clearCategory(io.pyhoma.domain.model.itil.Change.class, used);
        clearCategory(io.pyhoma.domain.model.itil.RecurringTicket.class, used);
        for (int i = nodes.size() - 1; i >= 0; i--) {
            purge(nodes.get(i));
        }
    }

    private <E extends io.pyhoma.domain.model.BaseEntity> void clearCategory(Class<E> type, Filter used) {
        for (E row : repo(type).find(used)) {
            if (row instanceof io.pyhoma.domain.model.itil.ItilItem i) {
                i.setCategoryId(null);
            } else if (row instanceof io.pyhoma.domain.model.itil.RecurringTicket r) {
                r.setCategoryId(null);
            }
            saveQuietly(row);
        }
    }

    public DropdownItem findOrCreate(DropdownKind kind, String name) {
        String clean = Texts.clean(name);
        if (clean == null) {
            return null;
        }
        return tx(() -> repo(DropdownItem.class).find(Filter.eq("kind", kind)).stream()
                .filter(d -> Texts.fold(d.getName()).equals(Texts.fold(clean))
                        || Texts.fold(d.getCompleteName()).equals(Texts.fold(clean)))
                .findFirst()
                .orElseGet(() -> {
                    DropdownItem d = new DropdownItem();
                    d.setKind(kind);
                    d.setName(clean);
                    return saveInternal(d);
                }));
    }

    DropdownItem create(DropdownKind kind, String name, Long parentId, String color, boolean flag, int order) {
        DropdownItem d = new DropdownItem();
        d.setKind(kind);
        d.setName(name);
        d.setParentId(parentId);
        d.setColor(color);
        d.setFlag(flag);
        d.setSortOrder(order);
        return saveInternal(d);
    }
}
