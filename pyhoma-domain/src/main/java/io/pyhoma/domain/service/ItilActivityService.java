package io.pyhoma.domain.service;

import io.pyhoma.domain.model.asset.Asset;
import io.pyhoma.domain.model.core.AuditAction;
import io.pyhoma.domain.model.core.AuditEntry;
import io.pyhoma.domain.model.itil.ApprovalStatus;
import io.pyhoma.domain.model.itil.Change;
import io.pyhoma.domain.model.itil.ItilApproval;
import io.pyhoma.domain.model.itil.ItilAsset;
import io.pyhoma.domain.model.itil.ItilFollowup;
import io.pyhoma.domain.model.itil.ItilItem;
import io.pyhoma.domain.model.itil.ItilObserver;
import io.pyhoma.domain.model.itil.ItilRelation;
import io.pyhoma.domain.model.itil.ItilSolution;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.ItilTask;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.RelationType;
import io.pyhoma.domain.model.itil.SolutionStatus;
import io.pyhoma.domain.model.itil.TaskStatus;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.service.NotificationService.Severity;
import io.pyhoma.domain.service.TimelineEntry.Kind;
import io.pyhoma.domain.support.AccessDeniedException;
import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class ItilActivityService extends AbstractService {

    public ItilActivityService(Services services) {
        super(services);
    }

    @SuppressWarnings("unchecked")
    private ItilService<ItilItem> svc(ItilType type) {
        return (ItilService<ItilItem>) services.itil(type);
    }

    private ItilItem item(ItilType type, Long id) {
        return svc(type).get(id);
    }

    private boolean isParticipant(ItilItem item) {
        Long me = currentUserId();
        if (me == null) {
            return true;
        }
        if (Objects.equals(item.getRequesterId(), me) || Objects.equals(item.getAssigneeId(), me)) {
            return true;
        }
        return repo(ItilObserver.class).exists(Filter.and(Filter.eq("itemType", item.itilType()),
                Filter.eq("itemId", item.getId()), Filter.eq("userId", me)));
    }

    private void requireWork(ItilType type) {
        require(svc(type).module(), Right.UPDATE);
    }

    public List<TimelineEntry> timeline(ItilType type, Long id) {
        ItilItem item = item(type, id);
        boolean privateOk = svc(type).canSeePrivate();
        Filter own = Filter.and(Filter.eq("itemType", type), Filter.eq("itemId", id));
        List<TimelineEntry> entries = new ArrayList<>();
        entries.add(new TimelineEntry(Kind.CREATED, null, item.getOpenedAt(), item.getRequesterId(), item.getContent(),
                false, null, null, 0, null, null));
        for (ItilFollowup f : repo(ItilFollowup.class).find(own)) {
            if (privateOk || !f.isPrivateNote()) {
                entries.add(new TimelineEntry(Kind.FOLLOWUP, f.getId(), f.getCreatedAt(), f.getAuthorId(), f.getContent(),
                        f.isPrivateNote(), null, null, 0, null, null));
            }
        }
        for (ItilTask t : repo(ItilTask.class).find(own)) {
            if (privateOk || !t.isPrivateNote()) {
                entries.add(new TimelineEntry(Kind.TASK, t.getId(), t.getCreatedAt(), t.getAuthorId(), t.getContent(),
                        t.isPrivateNote(), t.getStatus().name(), t.getTechnicianId(), t.getDurationMinutes(),
                        t.getPlannedStartAt(), t.getPlannedEndAt()));
            }
        }
        for (ItilSolution s : repo(ItilSolution.class).find(own)) {
            entries.add(new TimelineEntry(Kind.SOLUTION, s.getId(), s.getCreatedAt(), s.getAuthorId(), s.getContent(),
                    false, s.getStatus().name(), s.getAnsweredById(), 0, null, null));
        }
        for (ItilApproval a : repo(ItilApproval.class).find(own)) {
            String text = a.getRequestComment() == null ? "" : a.getRequestComment();
            if (a.getAnswerComment() != null) {
                text = text + (text.isEmpty() ? "" : "\n\n") + a.getAnswerComment();
            }
            entries.add(new TimelineEntry(Kind.APPROVAL, a.getId(), a.getCreatedAt(), a.getRequestedById(), text,
                    false, a.getStatus().name(), a.getApproverId(), 0, null, null));
        }
        for (AuditEntry e : repo(AuditEntry.class).find(Filter.and(Filter.eq("itemType", type.entityClass().getSimpleName()),
                Filter.eq("itemId", id), Filter.eq("fieldName", "status")))) {
            entries.add(new TimelineEntry(Kind.STATUS, e.getId(), e.getOccurredAt(), e.getUserId(), e.getOldValue(),
                    false, e.getNewValue(), null, 0, null, null));
        }
        entries.sort(Comparator.comparing(TimelineEntry::at, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(e -> e.kind().ordinal()));
        return entries;
    }

    public ItilFollowup addFollowup(ItilType type, Long id, String content, boolean privateNote) {
        return tx(() -> {
            ItilItem item = item(type, id);
            boolean technician = can(svc(type).module(), Right.UPDATE);
            if (!technician && !isParticipant(item)) {
                throw new AccessDeniedException(type.name(), "FOLLOWUP");
            }
            if (item.getStatus() == ItilStatus.CLOSED || item.getStatus() == ItilStatus.CANCELED) {
                throw new BusinessException("error.itemClosed");
            }
            String text = Texts.clean(content);
            Validator.create().required("content", text).throwIfInvalid();
            ItilFollowup f = new ItilFollowup();
            f.setItemType(type);
            f.setItemId(id);
            f.setAuthorId(currentUserId());
            f.setContent(text);
            f.setPrivateNote(privateNote && technician);
            ItilFollowup saved = insert(f);
            boolean requesterReopens = item.getStatus() == ItilStatus.SOLVED && Objects.equals(item.getRequesterId(), currentUserId());
            if (requesterReopens) {
                currentSolution(type, id).ifPresent(s -> {
                    s.setStatus(SolutionStatus.REFUSED);
                    s.setAnsweredById(currentUserId());
                    s.setAnsweredAt(now());
                    update(s);
                });
                svc(type).reopen(svc(type).reload(id));
            } else {
                if (technician && item instanceof Ticket t && t.getTakenAt() == null
                        && !Objects.equals(item.getRequesterId(), currentUserId())) {
                    t.setTakenAt(now());
                }
                svc(type).touch(item);
            }
            Set<Long> targets = svc(type).stakeholders(item, !saved.isPrivateNote());
            services.notifications().notify(targets, Severity.INFO, "notif.followup",
                    List.of(item.reference(), item.getTitle()), type.entityClass().getSimpleName(), id);
            return saved;
        });
    }

    public void deleteFollowup(Long followupId) {
        txRun(() -> {
            ItilFollowup f = repo(ItilFollowup.class).getById(followupId);
            item(f.getItemType(), f.getItemId());
            boolean own = Objects.equals(f.getAuthorId(), currentUserId());
            if (!own) {
                require(svc(f.getItemType()).module(), Right.DELETE);
            }
            purge(f);
        });
    }

    public List<ItilTask> tasks(ItilType type, Long id) {
        item(type, id);
        boolean privateOk = svc(type).canSeePrivate();
        return repo(ItilTask.class).find(Query.where(Filter.and(Filter.eq("itemType", type), Filter.eq("itemId", id)))
                .orderBy(Sort.asc("createdAt"))).stream().filter(t -> privateOk || !t.isPrivateNote()).toList();
    }

    public ItilTask saveTask(ItilTask task) {
        requireWork(task.getItemType());
        task.setContent(Texts.clean(task.getContent()));
        if (task.getDurationMinutes() == null) {
            task.setDurationMinutes(0);
        }
        Validator.create().required("content", task.getContent())
                .positive("durationMinutes", task.getDurationMinutes())
                .check(task.getPlannedStartAt() == null || task.getPlannedEndAt() == null
                        || !task.getPlannedEndAt().isBefore(task.getPlannedStartAt()), "plannedEndAt", "validation.endBeforeStart")
                .throwIfInvalid();
        return tx(() -> {
            ItilItem item = item(task.getItemType(), task.getItemId());
            int before = 0;
            Long previousTech = null;
            if (task.isNew()) {
                task.setAuthorId(currentUserId());
                if (task.getPlannedStartAt() != null && task.getPlannedEndAt() != null && task.getDurationMinutes() == 0) {
                    task.setDurationMinutes((int) Duration.between(task.getPlannedStartAt(), task.getPlannedEndAt()).toMinutes());
                }
            } else {
                ItilTask old = repo(ItilTask.class).getById(task.getId());
                before = old.getDurationMinutes();
                previousTech = old.getTechnicianId();
            }
            if (task.getStatus() == TaskStatus.DONE && task.getDoneAt() == null) {
                task.setDoneAt(now());
            }
            ItilTask saved = task.isNew() ? insert(task) : update(task);
            ItilItem fresh = svc(task.getItemType()).reload(item.getId());
            fresh.setActionTimeMinutes(Math.max(0, fresh.getActionTimeMinutes() - before + saved.getDurationMinutes()));
            if (saved.getPlannedStartAt() != null && fresh.getStatus() == ItilStatus.ASSIGNED) {
                svc(task.getItemType()).transition(fresh, ItilStatus.PLANNED);
            }
            if (fresh instanceof Ticket t && t.getTakenAt() == null) {
                t.setTakenAt(now());
            }
            fresh.setLastActivityAt(now());
            svc(task.getItemType()).saveForActivity(fresh);
            if (saved.getTechnicianId() != null && !Objects.equals(saved.getTechnicianId(), previousTech)) {
                services.notifications().notify(List.of(saved.getTechnicianId()), Severity.INFO, "notif.task.assigned",
                        List.of(item.reference(), item.getTitle()), task.getItemType().entityClass().getSimpleName(), item.getId());
            }
            return saved;
        });
    }

    public ItilTask completeTask(Long taskId) {
        ItilTask task = repo(ItilTask.class).getById(taskId);
        task.setStatus(TaskStatus.DONE);
        return saveTask(task);
    }

    public void deleteTask(Long taskId) {
        txRun(() -> {
            ItilTask t = repo(ItilTask.class).getById(taskId);
            require(svc(t.getItemType()).module(), Right.UPDATE);
            ItilItem item = svc(t.getItemType()).reload(t.getItemId());
            item.setActionTimeMinutes(Math.max(0, item.getActionTimeMinutes() - t.getDurationMinutes()));
            svc(t.getItemType()).saveForActivity(item);
            purge(t);
        });
    }

    public List<ItilTask> plannedTasks(Long technicianId, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return repo(ItilTask.class).find(Query.where(Filter.and(
                technicianId == null ? Filter.ALL : Filter.eq("technicianId", technicianId),
                Filter.notNull("plannedStartAt"), Filter.lt("plannedStartAt", to), Filter.ge("plannedEndAt", from)))
                .orderBy(Sort.asc("plannedStartAt")));
    }

    public Optional<ItilSolution> currentSolution(ItilType type, Long id) {
        return repo(ItilSolution.class).findFirst(Query.where(Filter.and(Filter.eq("itemType", type), Filter.eq("itemId", id),
                Filter.ne("status", SolutionStatus.REFUSED))).orderBy(Sort.desc("id")));
    }

    public ItilSolution proposeSolution(ItilType type, Long id, String content, Long solutionTypeId) {
        if (type == ItilType.CHANGE) {
            throw new BusinessException("error.solutionNotForChanges");
        }
        requireWork(type);
        String text = Texts.clean(content);
        Validator.create().required("content", text).throwIfInvalid();
        return tx(() -> {
            ItilItem item = item(type, id);
            if (item.getStatus().isTerminal()) {
                throw new BusinessException("error.itemClosed");
            }
            if (svc(type).hasOpenSolution(id)) {
                throw new BusinessException("error.solutionPending");
            }
            boolean autoAccept = Objects.equals(item.getRequesterId(), currentUserId()) || item.getRequesterId() == null;
            ItilSolution s = new ItilSolution();
            s.setItemType(type);
            s.setItemId(id);
            s.setAuthorId(currentUserId());
            s.setContent(text);
            s.setSolutionTypeId(solutionTypeId);
            s.setStatus(autoAccept ? SolutionStatus.ACCEPTED : SolutionStatus.PROPOSED);
            if (autoAccept) {
                s.setAnsweredById(currentUserId());
                s.setAnsweredAt(now());
            }
            ItilSolution saved = insert(s);
            ItilItem fresh = svc(type).reload(id);
            if (fresh instanceof Ticket t && t.getTakenAt() == null) {
                t.setTakenAt(now());
            }
            svc(type).solve(fresh, autoAccept);
            return saved;
        });
    }

    public void acceptSolution(ItilType type, Long id) {
        txRun(() -> {
            ItilItem item = item(type, id);
            checkCanAnswerSolution(type, item);
            ItilSolution s = currentSolution(type, id).filter(x -> x.getStatus() == SolutionStatus.PROPOSED)
                    .orElseThrow(() -> new BusinessException("error.noPendingSolution"));
            s.setStatus(SolutionStatus.ACCEPTED);
            s.setAnsweredById(currentUserId());
            s.setAnsweredAt(now());
            update(s);
            svc(type).close(svc(type).reload(id));
        });
    }

    void acceptPendingSolution(ItilType type, Long id) {
        txRun(() -> {
            currentSolution(type, id).filter(x -> x.getStatus() == SolutionStatus.PROPOSED).ifPresent(s -> {
                s.setStatus(SolutionStatus.ACCEPTED);
                s.setAnsweredAt(now());
                update(s);
            });
            ItilItem item = svc(type).reload(id);
            if (item.getStatus() == ItilStatus.SOLVED) {
                svc(type).close(item);
            }
        });
    }

    public void refuseSolution(ItilType type, Long id, String reason) {
        String text = Texts.clean(reason);
        Validator.create().required("reason", text).throwIfInvalid();
        txRun(() -> {
            ItilItem item = item(type, id);
            checkCanAnswerSolution(type, item);
            ItilSolution s = currentSolution(type, id).filter(x -> x.getStatus() == SolutionStatus.PROPOSED)
                    .orElseThrow(() -> new BusinessException("error.noPendingSolution"));
            s.setStatus(SolutionStatus.REFUSED);
            s.setAnsweredById(currentUserId());
            s.setAnsweredAt(now());
            update(s);
            ItilFollowup f = new ItilFollowup();
            f.setItemType(type);
            f.setItemId(id);
            f.setAuthorId(currentUserId());
            f.setContent(text);
            insert(f);
            svc(type).reopen(svc(type).reload(id));
        });
    }

    private void checkCanAnswerSolution(ItilType type, ItilItem item) {
        if (!Objects.equals(item.getRequesterId(), currentUserId()) && !can(svc(type).module(), Right.UPDATE)) {
            throw new AccessDeniedException(type.name(), "SOLUTION");
        }
    }

    public List<ItilApproval> approvals(ItilType type, Long id) {
        item(type, id);
        return repo(ItilApproval.class).find(Query.where(Filter.and(Filter.eq("itemType", type), Filter.eq("itemId", id)))
                .orderBy(Sort.asc("id")));
    }

    public List<ItilApproval> myPendingApprovals() {
        return repo(ItilApproval.class).find(Query.where(Filter.and(Filter.eq("approverId", currentUserId()),
                Filter.eq("status", ApprovalStatus.WAITING))).orderBy(Sort.asc("createdAt")));
    }

    public ItilApproval requestApproval(ItilType type, Long id, Long approverId, String comment) {
        if (type == ItilType.PROBLEM) {
            throw new BusinessException("error.approvalNotForProblems");
        }
        requireWork(type);
        item(type, id);
        if (Objects.equals(approverId, currentUserId())) {
            throw new BusinessException("error.selfApproval");
        }
        return requestApprovalInternal(type, id, approverId, comment);
    }

    ItilApproval requestApprovalInternal(ItilType type, Long id, Long approverId, String comment) {
        Validator.create().required("approverId", approverId).throwIfInvalid();
        return tx(() -> {
            if (repo(ItilApproval.class).exists(Filter.and(Filter.eq("itemType", type), Filter.eq("itemId", id),
                    Filter.eq("approverId", approverId), Filter.eq("status", ApprovalStatus.WAITING)))) {
                throw new BusinessException("error.approvalAlreadyRequested");
            }
            ItilApproval a = new ItilApproval();
            a.setItemType(type);
            a.setItemId(id);
            a.setApproverId(approverId);
            a.setRequestedById(currentUserId());
            a.setRequestComment(Texts.clean(comment));
            a.setStatus(ApprovalStatus.WAITING);
            ItilApproval saved = insert(a);
            ItilItem item = svc(type).reload(id);
            if (item instanceof Ticket t) {
                t.setApprovalStatus(ApprovalStatus.WAITING);
            } else if (item instanceof Change c) {
                c.setApprovalStatus(ApprovalStatus.WAITING);
                if (c.getStatus() == ItilStatus.NEW || c.getStatus() == ItilStatus.EVALUATION) {
                    svc(type).transition(c, ItilStatus.APPROVAL);
                }
            }
            svc(type).saveForActivity(item);
            services.notifications().notify(List.of(approverId), Severity.WARNING, "notif.approval.requested",
                    List.of(item.reference(), item.getTitle()), type.entityClass().getSimpleName(), id);
            return saved;
        });
    }

    public ItilApproval answerApproval(Long approvalId, boolean accept, String comment) {
        return tx(() -> {
            ItilApproval a = repo(ItilApproval.class).getById(approvalId);
            ItilType type = a.getItemType();
            boolean mine = Objects.equals(a.getApproverId(), currentUserId());
            if (!mine && !can(svc(type).module(), Right.APPROVE)) {
                throw new AccessDeniedException(type.name(), "APPROVE");
            }
            if (!mine && Objects.equals(a.getRequestedById(), currentUserId())) {
                throw new BusinessException("error.selfApproval");
            }
            if (a.getStatus() != ApprovalStatus.WAITING) {
                throw new BusinessException("error.approvalAlreadyAnswered");
            }
            if (!accept && Texts.isBlank(comment)) {
                throw new BusinessException("error.refusalNeedsComment");
            }
            a.setStatus(accept ? ApprovalStatus.ACCEPTED : ApprovalStatus.REFUSED);
            a.setAnswerComment(Texts.clean(comment));
            a.setAnsweredAt(now());
            ItilApproval saved = update(a);
            ApprovalStatus global = aggregate(type, a.getItemId());
            ItilItem item = svc(type).reload(a.getItemId());
            if (item instanceof Ticket t) {
                t.setApprovalStatus(global);
            } else if (item instanceof Change c) {
                services.changes().onApprovalResult(c, global);
            }
            svc(type).saveForActivity(item);
            Set<Long> targets = new LinkedHashSet<>();
            targets.add(a.getRequestedById());
            targets.add(item.getAssigneeId());
            services.notifications().notify(targets, accept ? Severity.SUCCESS : Severity.DANGER,
                    accept ? "notif.approval.accepted" : "notif.approval.refused",
                    List.of(item.reference(), item.getTitle()), type.entityClass().getSimpleName(), item.getId());
            return saved;
        });
    }

    private ApprovalStatus aggregate(ItilType type, Long id) {
        List<ItilApproval> all = repo(ItilApproval.class).find(Filter.and(Filter.eq("itemType", type), Filter.eq("itemId", id)));
        if (all.isEmpty()) {
            return ApprovalStatus.NONE;
        }
        if (all.stream().anyMatch(x -> x.getStatus() == ApprovalStatus.REFUSED)) {
            return ApprovalStatus.REFUSED;
        }
        if (all.stream().allMatch(x -> x.getStatus() == ApprovalStatus.ACCEPTED)) {
            return ApprovalStatus.ACCEPTED;
        }
        return ApprovalStatus.WAITING;
    }

    public ItilRelation link(ItilType sourceType, Long sourceId, ItilType targetType, Long targetId, RelationType relation) {
        requireWork(sourceType);
        if (sourceType == targetType && Objects.equals(sourceId, targetId)) {
            throw new BusinessException("error.selfLink");
        }
        return tx(() -> {
            item(sourceType, sourceId);
            item(targetType, targetId);
            boolean exists = repo(ItilRelation.class).exists(Filter.or(
                    Filter.and(Filter.eq("sourceType", sourceType), Filter.eq("sourceId", sourceId),
                            Filter.eq("targetType", targetType), Filter.eq("targetId", targetId)),
                    Filter.and(Filter.eq("sourceType", targetType), Filter.eq("sourceId", targetId),
                            Filter.eq("targetType", sourceType), Filter.eq("targetId", sourceId))));
            if (exists) {
                throw new BusinessException("error.alreadyLinked");
            }
            ItilRelation r = new ItilRelation();
            r.setSourceType(sourceType);
            r.setSourceId(sourceId);
            r.setTargetType(targetType);
            r.setTargetId(targetId);
            r.setRelationType(relation == null ? RelationType.LINKED : relation);
            r.setCreatedById(currentUserId());
            return insert(r);
        });
    }

    public void unlink(Long relationId) {
        txRun(() -> {
            ItilRelation r = repo(ItilRelation.class).getById(relationId);
            requireWork(r.getSourceType());
            purge(r);
        });
    }

    public List<RelatedItem> related(ItilType type, Long id) {
        item(type, id);
        List<RelatedItem> result = new ArrayList<>();
        for (ItilRelation r : repo(ItilRelation.class).find(Filter.or(
                Filter.and(Filter.eq("sourceType", type), Filter.eq("sourceId", id)),
                Filter.and(Filter.eq("targetType", type), Filter.eq("targetId", id))))) {
            boolean outgoing = r.getSourceType() == type && Objects.equals(r.getSourceId(), id);
            ItilType otherType = outgoing ? r.getTargetType() : r.getSourceType();
            Long otherId = outgoing ? r.getTargetId() : r.getSourceId();
            svc(otherType).find(otherId).ifPresent(o -> result.add(new RelatedItem(r.getId(), r.getRelationType(), outgoing,
                    otherType, otherId, o.getTitle(), o.getStatus(), o.getPriority())));
        }
        return result;
    }

    public void linkAsset(ItilType type, Long id, Long assetId) {
        txRun(() -> {
            ItilItem item = item(type, id);
            if (!can(svc(type).module(), Right.UPDATE) && !Objects.equals(item.getRequesterId(), currentUserId())) {
                throw new AccessDeniedException(type.name(), "UPDATE");
            }
            Asset asset = repo(Asset.class).getById(assetId);
            if (repo(ItilAsset.class).exists(Filter.and(Filter.eq("itemType", type), Filter.eq("itemId", id),
                    Filter.eq("assetId", assetId)))) {
                return;
            }
            ItilAsset link = new ItilAsset();
            link.setItemType(type);
            link.setItemId(id);
            link.setAssetId(assetId);
            saveQuietly(link);
            services.audit().record(item, AuditAction.LINK, "asset", null, asset.getName());
        });
    }

    public void unlinkAsset(ItilType type, Long id, Long assetId) {
        requireWork(type);
        txRun(() -> {
            ItilItem item = item(type, id);
            int n = repo(ItilAsset.class).deleteWhere(Filter.and(Filter.eq("itemType", type), Filter.eq("itemId", id),
                    Filter.eq("assetId", assetId)));
            if (n > 0) {
                services.audit().record(item, AuditAction.UNLINK, "asset", String.valueOf(assetId), null);
            }
        });
    }

    public List<Asset> assets(ItilType type, Long id) {
        item(type, id);
        List<Long> ids = repo(ItilAsset.class).find(Filter.and(Filter.eq("itemType", type), Filter.eq("itemId", id)))
                .stream().map(ItilAsset::getAssetId).toList();
        return repo(Asset.class).findAllById(ids);
    }

    public List<RelatedItem> itemsForAsset(Long assetId) {
        List<RelatedItem> result = new ArrayList<>();
        for (ItilAsset link : repo(ItilAsset.class).find(Query.where(Filter.eq("assetId", assetId)).orderBy(Sort.desc("id")))) {
            if (!can(svc(link.getItemType()).module(), Right.READ)) {
                continue;
            }
            svc(link.getItemType()).find(link.getItemId()).ifPresent(o -> result.add(new RelatedItem(null, RelationType.LINKED,
                    true, link.getItemType(), o.getId(), o.getTitle(), o.getStatus(), o.getPriority())));
        }
        return result;
    }

    public List<ItilObserver> observers(ItilType type, Long id) {
        item(type, id);
        return repo(ItilObserver.class).find(Query.where(Filter.and(Filter.eq("itemType", type), Filter.eq("itemId", id),
                        Filter.eq("actorRole", io.pyhoma.domain.model.itil.ItilActorRole.FOLLOWER)))
                .orderBy(Sort.asc("id")));
    }

    public ItilObserver addObserver(ItilType type, Long id, Long userId, Long groupId) {
        requireWork(type);
        item(type, id);
        return addObserverInternal(type, id, userId, groupId);
    }

    ItilObserver addObserverInternal(ItilType type, Long id, Long userId, Long groupId) {
        if (userId == null && groupId == null) {
            throw new BusinessException("validation.required");
        }
        return tx(() -> {
            Filter same = Filter.and(Filter.eq("itemType", type), Filter.eq("itemId", id), Filter.eq("userId", userId),
                    Filter.eq("groupId", groupId), Filter.eq("actorRole", io.pyhoma.domain.model.itil.ItilActorRole.FOLLOWER));
            Optional<ItilObserver> existing = repo(ItilObserver.class).findFirst(same);
            if (existing.isPresent()) {
                return existing.get();
            }
            ItilObserver o = new ItilObserver();
            o.setItemType(type);
            o.setItemId(id);
            o.setUserId(userId);
            o.setGroupId(groupId);
            return insert(o);
        });
    }

    public void removeObserver(Long observerId) {
        txRun(() -> {
            ItilObserver o = repo(ItilObserver.class).getById(observerId);
            boolean self = Objects.equals(o.getUserId(), currentUserId());
            if (!self) {
                requireWork(o.getItemType());
            }
            purge(o);
        });
    }
}
