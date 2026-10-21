package io.pyhoma.domain.service;

import io.pyhoma.domain.itil.PriorityMatrix;
import io.pyhoma.domain.model.core.AuditAction;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.core.User;
import io.pyhoma.domain.model.itil.ItilActorRole;
import io.pyhoma.domain.model.itil.ItilAsset;
import io.pyhoma.domain.model.itil.ItilFollowup;
import io.pyhoma.domain.model.itil.ItilItem;
import io.pyhoma.domain.model.itil.ItilObserver;
import io.pyhoma.domain.model.itil.ItilRelation;
import io.pyhoma.domain.model.itil.ItilSolution;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.ItilTask;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.ItilApproval;
import io.pyhoma.domain.model.itil.SolutionStatus;
import io.pyhoma.domain.model.itil.TaskStatus;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.PageResult;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.security.UserSession;
import io.pyhoma.domain.service.NotificationService.Severity;
import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.NotFoundException;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class ItilService<T extends ItilItem> extends AbstractService {

    protected ItilService(Services services) {
        super(services);
    }

    public abstract ItilType itilType();

    protected abstract Class<T> type();

    public abstract Module module();

    protected abstract T newInstance();

    protected void beforeCreate(T item) {
    }

    protected void afterCreate(T item) {
    }

    protected void beforeUpdate(T before, T after) {
    }

    protected void onStatusChange(T item, ItilStatus from, ItilStatus to) {
    }

    protected void onAssignment(T item) {
    }

    public T draft() {
        T item = newInstance();
        UserSession s = security().current();
        item.setOrgUnitId(s.activeOrgUnitId());
        item.setRequesterId(s.userId());
        item.setUrgency(3);
        item.setImpact(3);
        item.setPriority(3);
        item.setStatus(ItilStatus.NEW);
        return item;
    }

    public PageResult<T> search(ItilSearch s) {
        require(module(), Right.READ);
        Query q = Query.where(filter(s)).orderBy(new Sort(s.sortField() == null ? "id" : s.sortField(), s.ascending()))
                .page(s.pageIndex(), s.pageSize());
        return repo(type()).page(q);
    }

    public Map<ItilStatus, List<T>> board(ItilSearch s) {
        require(module(), Right.READ);
        Filter base = Filter.and(filter(s.view(ItilSearch.View.ALL)),
                Filter.or(Filter.notIn("status", List.of(ItilStatus.CLOSED, ItilStatus.CANCELED)),
                        Filter.ge("closedAt", now().minusDays(14))));
        List<T> items = repo(type()).find(Query.where(base).orderBy(Sort.desc("priority"), Sort.asc("openedAt")).limit(1000));
        Map<ItilStatus, List<T>> board = new EnumMap<>(ItilStatus.class);
        for (ItilStatus st : ItilStatus.workflow(itilType())) {
            board.put(st, new ArrayList<>());
        }
        for (T item : items) {
            board.computeIfAbsent(item.getStatus(), k -> new ArrayList<>()).add(item);
        }
        return board;
    }

    public long count(ItilSearch s) {
        if (!can(module(), Right.READ)) {
            return 0;
        }
        return repo(type()).count(filter(s));
    }

    public T get(Long id) {
        require(module(), Right.READ);
        T item = repo(type()).getById(id);
        checkVisible(item);
        if (!repo(type()).exists(Filter.and(Filter.eq("id", id), visibility()))) {
            throw new NotFoundException(type().getSimpleName(), id);
        }
        return item;
    }

    public java.util.Optional<T> find(Long id) {
        try {
            return java.util.Optional.of(get(id));
        } catch (NotFoundException e) {
            return java.util.Optional.empty();
        }
    }

    protected Filter filter(ItilSearch s) {
        Long me = currentUserId();
        LocalDateTime now = now();
        Set<Long> myGroups = security().current().groupIds();
        Filter view = switch (s.view()) {
            case ALL -> Filter.isFalse("deleted");
            case OPEN -> Filter.and(Filter.isFalse("deleted"), Filter.in("status", ItilStatus.open(itilType())));
            case MINE_ASSIGNED -> Filter.and(Filter.isFalse("deleted"), assignedTo(me),
                    Filter.in("status", ItilStatus.open(itilType())));
            case MY_GROUPS -> Filter.and(Filter.isFalse("deleted"), Filter.in("assigneeGroupId", myGroups),
                    Filter.in("status", ItilStatus.open(itilType())));
            case MINE_REQUESTED -> Filter.and(Filter.isFalse("deleted"), Filter.eq("requesterId", me));
            case UNASSIGNED -> Filter.and(Filter.isFalse("deleted"), Filter.isNull("assigneeId"),
                    Filter.isNull("assigneeGroupId"), Filter.in("status", ItilStatus.open(itilType())));
            case OVERDUE -> Filter.and(Filter.isFalse("deleted"), Filter.in("status", ItilStatus.open(itilType())),
                    Filter.lt("dueAt", now));
            case SOLVED -> Filter.and(Filter.isFalse("deleted"), Filter.eq("status", ItilStatus.SOLVED));
            case CLOSED -> Filter.and(Filter.isFalse("deleted"), Filter.in("status", List.of(ItilStatus.CLOSED, ItilStatus.CANCELED)));
            case TRASH -> Filter.isTrue("deleted");
        };
        List<Filter> parts = new ArrayList<>();
        parts.add(view);
        parts.add(security().orgFilter());
        parts.add(visibility());
        parts.add(Filter.search(s.text(), "title", "content"));
        if (s.text() != null && s.text().trim().matches("#?\\d+")) {
            parts.set(parts.size() - 1, Filter.or(Filter.search(s.text(), "title", "content"),
                    Filter.eq("id", Long.valueOf(s.text().trim().replace("#", "")))));
        }
        if (!s.statuses().isEmpty()) {
            parts.add(Filter.in("status", s.statuses()));
        }
        if (s.assigneeId() != null) {
            parts.add(assignedTo(s.assigneeId()));
        }
        if (s.assigneeGroupId() != null) {
            parts.add(Filter.eq("assigneeGroupId", s.assigneeGroupId()));
        }
        if (s.requesterId() != null) {
            parts.add(Filter.eq("requesterId", s.requesterId()));
        }
        if (s.categoryId() != null) {
            parts.add(Filter.eq("categoryId", s.categoryId()));
        }
        if (s.locationId() != null) {
            parts.add(Filter.eq("locationId", s.locationId()));
        }
        if (s.minPriority() != null) {
            parts.add(Filter.ge("priority", s.minPriority()));
        }
        if (s.openedFrom() != null) {
            parts.add(Filter.ge("openedAt", s.openedFrom().atStartOfDay()));
        }
        if (s.openedTo() != null) {
            parts.add(Filter.lt("openedAt", s.openedTo().plusDays(1).atStartOfDay()));
        }
        parts.add(extraFilter(s));
        return Filter.and(parts);
    }

    protected Filter assignedTo(Long userId) {
        Set<Long> extra = new LinkedHashSet<>();
        for (ItilObserver o : repo(ItilObserver.class).find(Filter.and(Filter.eq("itemType", itilType()),
                Filter.eq("userId", userId), Filter.eq("actorRole", ItilActorRole.ASSIGNEE)))) {
            extra.add(o.getItemId());
        }
        return extra.isEmpty() ? Filter.eq("assigneeId", userId) : Filter.or(Filter.eq("assigneeId", userId), Filter.in("id", extra));
    }

    protected Filter extraFilter(ItilSearch s) {
        return Filter.ALL;
    }

    protected Filter visibility() {
        UserSession s = security().current();
        if (s.isSystem() || !module().supports(Right.SEE_ALL) || s.can(module(), Right.SEE_ALL)) {
            return Filter.ALL;
        }
        Set<Long> observed = new LinkedHashSet<>();
        Filter observerFilter = Filter.and(Filter.eq("itemType", itilType()), Filter.or(Filter.eq("userId", s.userId()),
                Filter.in("groupId", s.groupIds())));
        for (ItilObserver o : repo(ItilObserver.class).find(observerFilter)) {
            observed.add(o.getItemId());
        }
        return Filter.or(Filter.eq("requesterId", s.userId()), Filter.eq("assigneeId", s.userId()),
                Filter.in("assigneeGroupId", s.groupIds()), Filter.in("id", observed));
    }

    public boolean canSeePrivate() {
        return can(module(), Right.UPDATE);
    }

    public T create(T item) {
        require(module(), Right.CREATE);
        normalize(item);
        if (item.getOrgUnitId() == null) {
            item.setOrgUnitId(security().current().activeOrgUnitId());
        }
        if (!security().current().canSeeOrgUnit(item.getOrgUnitId())) {
            throw new BusinessException("error.orgUnitNotAllowed");
        }
        if (!can(module(), Right.ASSIGN) && !can(module(), Right.UPDATE)) {
            item.setAssigneeGroupId(null);
        }
        if (item.getAssigneeId() != null) {
            checkPeople(List.of(item.getAssigneeId()));
        }
        validate(item);
        return tx(() -> {
            LocalDateTime now = now();
            if (item.getOpenedAt() == null) {
                item.setOpenedAt(now);
            }
            item.setLastActivityAt(now);
            item.setStatus(ItilStatus.NEW);
            boolean major = item.getPriority() != null && item.getPriority() == PriorityMatrix.MAJOR
                    && can(module(), Right.UPDATE);
            item.setPriority(major ? PriorityMatrix.MAJOR : PriorityMatrix.compute(item.getUrgency(), item.getImpact()));
            beforeCreate(item);
            if (item.getAssigneeId() != null || item.getAssigneeGroupId() != null) {
                autoAssignStatus(item);
                onAssignment(item);
            }
            T saved = insert(item);
            afterCreate(saved);
            notifyCreated(saved);
            return repo(type()).getById(saved.getId());
        });
    }

    public T save(T item) {
        if (item.isNew()) {
            return create(item);
        }
        require(module(), Right.UPDATE);
        normalize(item);
        validate(item);
        return tx(() -> {
            T before = get(item.getId());
            if (!Objects.equals(before.getUrgency(), item.getUrgency()) || !Objects.equals(before.getImpact(), item.getImpact())) {
                if (Objects.equals(before.getPriority(), item.getPriority())) {
                    item.setPriority(PriorityMatrix.compute(item.getUrgency(), item.getImpact()));
                }
            }
            boolean assignmentChanged = !Objects.equals(before.getAssigneeId(), item.getAssigneeId())
                    || !Objects.equals(before.getAssigneeGroupId(), item.getAssigneeGroupId());
            if (assignmentChanged && !can(module(), Right.ASSIGN) && !Objects.equals(item.getAssigneeId(), currentUserId())) {
                throw new BusinessException("error.cannotAssign");
            }
            ItilStatus target = item.getStatus();
            item.setStatus(before.getStatus());
            if (assignmentChanged) {
                autoAssignStatus(item);
                onAssignment(item);
            }
            if (target != before.getStatus()) {
                checkManualTransition(before, target);
                transition(item, target);
            }
            item.setLastActivityAt(now());
            beforeUpdate(before, item);
            T saved = update(item);
            if (assignmentChanged) {
                notifyAssigned(saved);
            }
            if (before.getStatus() != saved.getStatus()) {
                notifyStatus(saved);
            }
            return saved;
        });
    }

    public T changeStatus(Long id, ItilStatus status) {
        T item = get(id);
        item.setStatus(status);
        return save(item);
    }

    public T createInStatus(T item, ItilStatus status) {
        return tx(() -> {
            T created = create(item);
            if (status == null || status == ItilStatus.NEW || status == created.getStatus() || status.isTerminal()
                    || !can(module(), Right.UPDATE)) {
                return created;
            }
            return changeStatus(created.getId(), status);
        });
    }

    public T createWithPeople(T item, ItilStatus status, Collection<Long> assignees, Collection<Long> followers) {
        List<Long> who = distinct(assignees);
        List<Long> watch = distinct(followers);
        watch.removeAll(who);
        checkPeople(who);
        checkPeople(watch);
        return tx(() -> {
            item.setAssigneeId(who.isEmpty() ? null : who.get(0));
            T created = createInStatus(item, status);
            Set<Long> extra = new LinkedHashSet<>();
            for (int i = 1; i < who.size(); i++) {
                addActor(created.getId(), who.get(i), ItilActorRole.ASSIGNEE);
                extra.add(who.get(i));
            }
            for (Long u : watch) {
                addActor(created.getId(), u, ItilActorRole.FOLLOWER);
            }
            services.notifications().notify(extra, Severity.INFO, key("assigned"),
                    List.of(created.reference(), created.getTitle()), type().getSimpleName(), created.getId());
            return repo(type()).getById(created.getId());
        });
    }

    public ItilPeople people(Long id) {
        T item = get(id);
        return peopleOf(item);
    }

    private ItilPeople peopleOf(T item) {
        List<Long> assignees = new ArrayList<>();
        List<Long> followers = new ArrayList<>();
        if (item.getAssigneeId() != null) {
            assignees.add(item.getAssigneeId());
        }
        for (ItilObserver o : actors(List.of(item.getId()))) {
            if (o.getUserId() == null) {
                continue;
            }
            List<Long> target = o.getActorRole() == ItilActorRole.ASSIGNEE ? assignees : followers;
            if (!target.contains(o.getUserId())) {
                target.add(o.getUserId());
            }
        }
        followers.removeAll(assignees);
        return new ItilPeople(assignees, followers);
    }

    public ItilPeople setPeople(Long id, Collection<Long> assignees, Collection<Long> followers) {
        List<Long> who = distinct(assignees);
        List<Long> watch = distinct(followers);
        watch.removeAll(who);
        checkPeople(who);
        checkPeople(watch);
        return tx(() -> {
            T item = get(id);
            if (item.getStatus().isTerminal()) {
                throw new BusinessException("error.itemClosed");
            }
            ItilPeople before = peopleOf(item);
            Long primary = who.isEmpty() ? null : who.get(0);
            if (!Objects.equals(primary, item.getAssigneeId())) {
                item.setAssigneeId(primary);
                autoAssignStatus(item);
                if (primary != null) {
                    onAssignment(item);
                }
                item.setLastActivityAt(now());
                update(item);
            }
            for (ItilObserver o : actors(List.of(id))) {
                if (o.getUserId() != null) {
                    purge(o);
                }
            }
            for (int i = 1; i < who.size(); i++) {
                addActor(id, who.get(i), ItilActorRole.ASSIGNEE);
            }
            for (Long u : watch) {
                addActor(id, u, ItilActorRole.FOLLOWER);
            }
            Set<Long> fresh = new LinkedHashSet<>(who);
            fresh.removeAll(before.assignees());
            fresh.remove(currentUserId());
            T saved = repo(type()).getById(id);
            services.notifications().notify(fresh, Severity.INFO, key("assigned"),
                    List.of(saved.reference(), saved.getTitle()), type().getSimpleName(), saved.getId());
            return peopleOf(saved);
        });
    }

    public Map<Long, ItilCard> cards(Collection<T> items) {
        Map<Long, ItilCard> result = new HashMap<>();
        if (items.isEmpty()) {
            return result;
        }
        List<Long> ids = items.stream().map(ItilItem::getId).toList();
        Map<Long, List<Long>> extra = new HashMap<>();
        for (ItilObserver o : actors(ids)) {
            if (o.getActorRole() == ItilActorRole.ASSIGNEE && o.getUserId() != null) {
                extra.computeIfAbsent(o.getItemId(), k -> new ArrayList<>()).add(o.getUserId());
            }
        }
        Map<Long, int[]> tasks = new HashMap<>();
        for (ItilTask t : repo(ItilTask.class).find(Filter.and(Filter.eq("itemType", itilType()), Filter.in("itemId", ids)))) {
            if (t.getStatus() == TaskStatus.INFO) {
                continue;
            }
            int[] c = tasks.computeIfAbsent(t.getItemId(), k -> new int[2]);
            c[1]++;
            if (t.getStatus() == TaskStatus.DONE) {
                c[0]++;
            }
        }
        for (T item : items) {
            List<Long> who = new ArrayList<>();
            if (item.getAssigneeId() != null) {
                who.add(item.getAssigneeId());
            }
            for (Long u : extra.getOrDefault(item.getId(), List.of())) {
                if (!who.contains(u)) {
                    who.add(u);
                }
            }
            int[] c = tasks.getOrDefault(item.getId(), new int[2]);
            result.put(item.getId(), new ItilCard(who, c[0], c[1]));
        }
        return result;
    }

    private List<ItilObserver> actors(Collection<Long> ids) {
        return repo(ItilObserver.class).find(Query.where(Filter.and(Filter.eq("itemType", itilType()), Filter.in("itemId", ids)))
                .orderBy(Sort.asc("id")));
    }

    private void addActor(Long itemId, Long userId, ItilActorRole role) {
        ItilObserver o = new ItilObserver();
        o.setItemType(itilType());
        o.setItemId(itemId);
        o.setUserId(userId);
        o.setActorRole(role);
        insert(o);
    }

    private static List<Long> distinct(Collection<Long> ids) {
        List<Long> out = new ArrayList<>();
        if (ids != null) {
            for (Long id : ids) {
                if (id != null && !out.contains(id)) {
                    out.add(id);
                }
            }
        }
        return out;
    }

    private void checkPeople(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        List<User> users = repo(User.class).findAllById(List.copyOf(ids));
        if (users.size() != ids.size() || users.stream().anyMatch(u -> !u.isActive())) {
            throw new BusinessException("error.invalidPeople");
        }
    }

    public T assign(Long id, Long userId, Long groupId) {
        T item = get(id);
        item.setAssigneeId(userId);
        item.setAssigneeGroupId(groupId);
        return save(item);
    }

    public T takeIntoAccount(Long id) {
        T item = get(id);
        return assign(id, currentUserId(), item.getAssigneeGroupId());
    }

    public void trash(Long id) {
        require(module(), Right.DELETE);
        txRun(() -> {
            T item = get(id);
            item.setDeleted(true);
            saveQuietly(item);
            services.audit().record(item, AuditAction.DELETE, null, null, null);
        });
    }

    public void restore(Long id) {
        require(module(), Right.DELETE);
        txRun(() -> {
            T item = repo(type()).getById(id);
            checkVisible(item);
            item.setDeleted(false);
            saveQuietly(item);
            services.audit().record(item, AuditAction.RESTORE, null, null, null);
        });
    }

    public void purgeItem(Long id) {
        require(module(), Right.PURGE);
        txRun(() -> {
            T item = repo(type()).getById(id);
            checkVisible(item);
            ItilType t = itilType();
            Filter own = Filter.and(Filter.eq("itemType", t), Filter.eq("itemId", id));
            repo(ItilFollowup.class).deleteWhere(own);
            repo(ItilTask.class).deleteWhere(own);
            repo(ItilSolution.class).deleteWhere(own);
            repo(ItilApproval.class).deleteWhere(own);
            repo(ItilAsset.class).deleteWhere(own);
            repo(ItilObserver.class).deleteWhere(own);
            repo(ItilRelation.class).deleteWhere(Filter.or(
                    Filter.and(Filter.eq("sourceType", t), Filter.eq("sourceId", id)),
                    Filter.and(Filter.eq("targetType", t), Filter.eq("targetId", id))));
            purge(item);
        });
    }

    protected void checkManualTransition(T before, ItilStatus target) {
        if (!ItilStatus.workflow(itilType()).contains(target)) {
            throw new BusinessException("error.invalidStatus", target);
        }
        if (itilType() != ItilType.CHANGE) {
            if (target == ItilStatus.SOLVED) {
                throw new BusinessException("error.solutionRequired");
            }
            if (target == ItilStatus.CLOSED && before.getStatus() != ItilStatus.SOLVED) {
                throw new BusinessException("error.solutionRequired");
            }
        }
    }

    protected void transition(T item, ItilStatus target) {
        ItilStatus from = item.getStatus();
        if (from == target) {
            return;
        }
        LocalDateTime now = now();
        onStatusChange(item, from, target);
        switch (target) {
            case SOLVED -> item.setSolvedAt(now);
            case CLOSED, CANCELED -> {
                if (item.getSolvedAt() == null) {
                    item.setSolvedAt(now);
                }
                item.setClosedAt(now);
            }
            default -> {
                item.setSolvedAt(null);
                item.setClosedAt(null);
            }
        }
        item.setStatus(target);
    }

    protected void autoAssignStatus(T item) {
        if (item.getStatus() == ItilStatus.NEW && itilType() != ItilType.CHANGE
                && (item.getAssigneeId() != null || item.getAssigneeGroupId() != null)) {
            transition(item, ItilStatus.ASSIGNED);
        }
    }

    protected ItilStatus reopenStatus(T item) {
        return item.getAssigneeId() != null || item.getAssigneeGroupId() != null ? ItilStatus.ASSIGNED : ItilStatus.NEW;
    }

    void solve(T item, boolean autoAccept) {
        transition(item, ItilStatus.SOLVED);
        if (autoAccept) {
            transition(item, ItilStatus.CLOSED);
        }
        item.setLastActivityAt(now());
        update(item);
        notifyStatus(item);
    }

    void close(T item) {
        transition(item, ItilStatus.CLOSED);
        item.setLastActivityAt(now());
        update(item);
        afterClosed(item);
    }

    protected void afterClosed(T item) {
    }

    void reopen(T item) {
        transition(item, reopenStatus(item));
        item.setLastActivityAt(now());
        update(item);
        notifyAssigned(item);
    }

    void touch(T item) {
        item.setLastActivityAt(now());
        saveQuietly(item);
    }

    T reload(Long id) {
        return repo(type()).getById(id);
    }

    void saveForActivity(T item) {
        update(item);
    }

    boolean hasOpenSolution(Long id) {
        return repo(ItilSolution.class).exists(Filter.and(Filter.eq("itemType", itilType()), Filter.eq("itemId", id),
                Filter.eq("status", SolutionStatus.PROPOSED)));
    }

    protected void normalize(T item) {
        item.setTitle(Texts.clean(item.getTitle()));
        item.setContent(Texts.clean(item.getContent()));
        if (item.getUrgency() == null) {
            item.setUrgency(3);
        }
        if (item.getImpact() == null) {
            item.setImpact(3);
        }
    }

    protected void validate(T item) {
        Validator.create()
                .required("title", item.getTitle())
                .maxLength("title", item.getTitle(), 255)
                .range("urgency", item.getUrgency(), 1, 5)
                .range("impact", item.getImpact(), 1, 5)
                .range("priority", item.getPriority(), 1, 6)
                .throwIfInvalid();
    }

    protected Set<Long> stakeholders(T item, boolean includeRequester) {
        Set<Long> ids = new LinkedHashSet<>();
        if (includeRequester && item.getRequesterId() != null) {
            ids.add(item.getRequesterId());
        }
        if (item.getAssigneeId() != null) {
            ids.add(item.getAssigneeId());
        }
        for (ItilObserver o : repo(ItilObserver.class).find(Filter.and(Filter.eq("itemType", itilType()),
                Filter.eq("itemId", item.getId())))) {
            if (o.getUserId() != null) {
                ids.add(o.getUserId());
            }
            if (o.getGroupId() != null) {
                ids.addAll(services.groups().memberIds(o.getGroupId()));
            }
        }
        return ids;
    }

    protected String key(String suffix) {
        return "notif." + itilType().name().toLowerCase(java.util.Locale.ROOT) + "." + suffix;
    }

    protected void notifyCreated(T item) {
        Set<Long> targets = new LinkedHashSet<>();
        if (item.getAssigneeId() != null) {
            targets.add(item.getAssigneeId());
        } else if (item.getAssigneeGroupId() != null) {
            targets.addAll(services.groups().memberIds(item.getAssigneeGroupId()));
        }
        services.notifications().notify(targets, Severity.INFO, key("created"),
                List.of(item.reference(), item.getTitle()), type().getSimpleName(), item.getId());
    }

    protected void notifyAssigned(T item) {
        Set<Long> targets = new LinkedHashSet<>();
        if (item.getAssigneeId() != null) {
            targets.add(item.getAssigneeId());
        } else if (item.getAssigneeGroupId() != null) {
            targets.addAll(services.groups().memberIds(item.getAssigneeGroupId()));
        }
        services.notifications().notify(targets, Severity.INFO, key("assigned"),
                List.of(item.reference(), item.getTitle()), type().getSimpleName(), item.getId());
    }

    protected void notifyStatus(T item) {
        Severity sev = item.getStatus() == ItilStatus.SOLVED ? Severity.SUCCESS : Severity.INFO;
        services.notifications().notify(stakeholders(item, true), sev, key("status"),
                List.of(item.reference(), item.getTitle(), item.getStatus().name()), type().getSimpleName(), item.getId());
    }
}
