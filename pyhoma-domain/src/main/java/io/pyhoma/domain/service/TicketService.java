package io.pyhoma.domain.service;

import io.pyhoma.domain.itil.SlaState;
import io.pyhoma.domain.model.core.AuditAction;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.itil.ItilStatus;
import io.pyhoma.domain.model.itil.ItilType;
import io.pyhoma.domain.model.itil.RecurringTicket;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.model.rules.RuleTrigger;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.rules.RuleOutcome;
import io.pyhoma.domain.service.NotificationService.Severity;
import io.pyhoma.domain.support.BusinessException;
import io.pyhoma.domain.support.Texts;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TicketService extends ItilService<Ticket> {

    private final ThreadLocal<RuleOutcome> pendingOutcome = new ThreadLocal<>();

    public TicketService(Services services) {
        super(services);
    }

    @Override
    public ItilType itilType() {
        return ItilType.TICKET;
    }

    @Override
    protected Class<Ticket> type() {
        return Ticket.class;
    }

    @Override
    public Module module() {
        return Module.TICKET;
    }

    @Override
    protected Ticket newInstance() {
        return new Ticket();
    }

    @Override
    protected Filter extraFilter(ItilSearch s) {
        return s.ticketType() == null ? Filter.ALL : Filter.eq("ticketType", s.ticketType());
    }

    @Override
    protected void beforeCreate(Ticket ticket) {
        if (ticket.getWaitingMinutes() == null) {
            ticket.setWaitingMinutes(0);
        }
        RuleOutcome outcome = services.rules().apply(ticket, RuleTrigger.ON_CREATE);
        pendingOutcome.set(outcome);
        services.slas().apply(ticket);
    }

    @Override
    protected void afterCreate(Ticket ticket) {
        RuleOutcome outcome = pendingOutcome.get();
        pendingOutcome.remove();
        if (outcome == null) {
            return;
        }
        for (String rule : outcome.appliedRules()) {
            services.audit().record(ticket, AuditAction.UPDATE, "rule", null, rule);
        }
        security().runAsSystem(() -> {
            for (Long observer : outcome.observers()) {
                services.itilActivity().addObserverInternal(ItilType.TICKET, ticket.getId(), observer, null);
            }
            for (Long approver : outcome.approvers()) {
                services.itilActivity().requestApprovalInternal(ItilType.TICKET, ticket.getId(), approver,
                        "rule: " + String.join(", ", outcome.appliedRules()));
            }
        });
    }

    @Override
    protected void onAssignment(Ticket ticket) {
        if (ticket.getTakenAt() == null && ticket.getAssigneeId() != null) {
            ticket.setTakenAt(now());
        }
    }

    @Override
    protected void onStatusChange(Ticket ticket, ItilStatus from, ItilStatus to) {
        if (to == ItilStatus.WAITING) {
            services.slas().pause(ticket);
        } else if (from == ItilStatus.WAITING) {
            services.slas().resume(ticket);
        }
        if (ticket.getTakenAt() == null && to != ItilStatus.NEW && to != ItilStatus.CANCELED
                && ticket.getAssigneeId() != null) {
            ticket.setTakenAt(now());
        }
    }

    @Override
    protected void beforeUpdate(Ticket before, Ticket after) {
        boolean relevant = !Objects.equals(before.getCategoryId(), after.getCategoryId())
                || !Objects.equals(before.getTitle(), after.getTitle())
                || !Objects.equals(before.getUrgency(), after.getUrgency())
                || !Objects.equals(before.getImpact(), after.getImpact());
        if (relevant) {
            RuleOutcome outcome = services.rules().apply(after, RuleTrigger.ON_UPDATE);
            for (String rule : outcome.appliedRules()) {
                services.audit().record(after, AuditAction.UPDATE, "rule", null, rule);
            }
        }
        boolean slaChanged = !Objects.equals(before.getSlaTtrId(), after.getSlaTtrId())
                || !Objects.equals(before.getSlaTtoId(), after.getSlaTtoId())
                || !Objects.equals(before.getPriority(), after.getPriority());
        if (slaChanged && after.getStatus().isOpen()) {
            if (!Objects.equals(before.getPriority(), after.getPriority())
                    && Objects.equals(before.getSlaTtrId(), after.getSlaTtrId())) {
                after.setSlaTtoId(null);
                after.setSlaTtrId(null);
            }
            services.slas().apply(after);
            after.setSlaWarned(false);
            after.setSlaBreachNotified(false);
        }
    }

    @Override
    protected void afterClosed(Ticket ticket) {
        if (services.settings().getBoolean(SettingKeys.TICKET_SATISFACTION, true) && ticket.getRequesterId() != null) {
            services.notifications().notify(List.of(ticket.getRequesterId()), Severity.INFO, "notif.ticket.satisfaction",
                    List.of(ticket.reference(), ticket.getTitle()), "Ticket", ticket.getId());
        }
    }

    public Ticket rate(Long ticketId, int score, String comment) {
        return tx(() -> {
            Ticket t = get(ticketId);
            if (!Objects.equals(t.getRequesterId(), currentUserId())) {
                throw new BusinessException("error.onlyRequesterCanRate");
            }
            if (t.getStatus() != ItilStatus.CLOSED) {
                throw new BusinessException("error.ticketNotClosed");
            }
            t.setSatisfaction(Math.max(1, Math.min(5, score)));
            t.setSatisfactionComment(Texts.abbreviate(Texts.clean(comment), 2000));
            return update(t);
        });
    }

    public SlaState ttoState(Ticket t) {
        return services.slas().ttoState(t);
    }

    public SlaState ttrState(Ticket t) {
        return services.slas().ttrState(t);
    }

    int monitorSla() {
        int sent = 0;
        List<Ticket> open = repo(Ticket.class).find(Query.where(Filter.and(Filter.isFalse("deleted"),
                Filter.in("status", ItilStatus.open(ItilType.TICKET)), Filter.notNull("dueAt"))));
        for (Ticket t : open) {
            SlaState state = ttrState(t);
            Set<Long> targets = responsible(t);
            if (state == SlaState.BREACHED && !t.isSlaBreachNotified()) {
                if (t.getAssigneeGroupId() != null) {
                    targets.addAll(services.groups().managerIds(t.getAssigneeGroupId()));
                }
                services.notifications().notify(targets, Severity.DANGER, "notif.ticket.slaBreached",
                        List.of(t.reference(), t.getTitle()), "Ticket", t.getId());
                t.setSlaBreachNotified(true);
                t.setSlaWarned(true);
                saveQuietly(t);
                sent++;
            } else if (state == SlaState.AT_RISK && !t.isSlaWarned()) {
                services.notifications().notify(targets, Severity.WARNING, "notif.ticket.slaAtRisk",
                        List.of(t.reference(), t.getTitle()), "Ticket", t.getId());
                t.setSlaWarned(true);
                saveQuietly(t);
                sent++;
            }
        }
        return sent;
    }

    private Set<Long> responsible(Ticket t) {
        Set<Long> ids = new LinkedHashSet<>();
        if (t.getAssigneeId() != null) {
            ids.add(t.getAssigneeId());
        } else if (t.getAssigneeGroupId() != null) {
            ids.addAll(services.groups().memberIds(t.getAssigneeGroupId()));
        }
        return ids;
    }

    int autoClose() {
        int days = services.settings().getInt(SettingKeys.TICKET_AUTO_CLOSE_DAYS, 7);
        if (days <= 0) {
            return 0;
        }
        LocalDateTime limit = now().minusDays(days);
        int closed = 0;
        for (Ticket t : repo(Ticket.class).find(Filter.and(Filter.eq("status", ItilStatus.SOLVED), Filter.lt("solvedAt", limit),
                Filter.isFalse("deleted")))) {
            services.itilActivity().acceptPendingSolution(ItilType.TICKET, t.getId());
            closed++;
        }
        return closed;
    }

    Ticket openFromTemplate(RecurringTicket r) {
        Ticket t = new Ticket();
        t.setOrgUnitId(r.getOrgUnitId());
        t.setTitle(r.getTitle());
        t.setContent(r.getContent());
        t.setTicketType(r.getTicketType());
        t.setCategoryId(r.getCategoryId());
        t.setUrgency(r.getUrgency());
        t.setImpact(r.getImpact());
        t.setPriority(null);
        t.setAssigneeId(r.getAssigneeId());
        t.setAssigneeGroupId(r.getAssigneeGroupId());
        t.setRecurringId(r.getId());
        return create(t);
    }
}
