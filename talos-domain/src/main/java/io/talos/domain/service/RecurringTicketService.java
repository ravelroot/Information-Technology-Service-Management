package io.talos.domain.service;

import io.talos.domain.model.core.Module;
import io.talos.domain.model.itil.RecurringTicket;
import io.talos.domain.model.itil.Ticket;
import io.talos.domain.query.Filter;
import io.talos.domain.query.Query;
import io.talos.domain.query.Sort;
import io.talos.domain.security.Right;
import io.talos.domain.support.Texts;
import io.talos.domain.support.Validator;
import java.time.LocalDateTime;
import java.util.List;

public class RecurringTicketService extends AbstractService {

    public RecurringTicketService(Services services) {
        super(services);
    }

    public List<RecurringTicket> all() {
        require(Module.RECURRING_TICKET, Right.READ);
        return repo(RecurringTicket.class).find(Query.where(security().orgFilter()).orderBy(Sort.asc("nextRunAt")));
    }

    public RecurringTicket get(Long id) {
        require(Module.RECURRING_TICKET, Right.READ);
        RecurringTicket r = repo(RecurringTicket.class).getById(id);
        checkVisible(r);
        return r;
    }

    public RecurringTicket save(RecurringTicket r) {
        require(Module.RECURRING_TICKET, r.isNew() ? Right.CREATE : Right.UPDATE);
        r.setName(Texts.clean(r.getName()));
        r.setTitle(Texts.clean(r.getTitle()));
        if (r.getOrgUnitId() == null) {
            r.setOrgUnitId(security().current().activeOrgUnitId());
        }
        Validator.create().required("name", r.getName()).required("title", r.getTitle())
                .required("periodicity", r.getPeriodicity())
                .check(r.getIntervalCount() != null && r.getIntervalCount() > 0, "intervalCount", "validation.positive")
                .range("urgency", r.getUrgency(), 1, 5).range("impact", r.getImpact(), 1, 5)
                .throwIfInvalid();
        if (r.getNextRunAt() == null) {
            r.setNextRunAt(now().plusMinutes(1));
        }
        return tx(() -> persist(r));
    }

    public void delete(Long id) {
        require(Module.RECURRING_TICKET, Right.DELETE);
        txRun(() -> purge(get(id)));
    }

    public Ticket runNow(Long id) {
        require(Module.RECURRING_TICKET, Right.UPDATE);
        return tx(() -> fire(get(id)));
    }

    static LocalDateTime next(LocalDateTime from, RecurringTicket r) {
        int n = Math.max(1, r.getIntervalCount());
        return switch (r.getPeriodicity()) {
            case DAILY -> from.plusDays(n);
            case WEEKLY -> from.plusWeeks(n);
            case MONTHLY -> from.plusMonths(n);
            case YEARLY -> from.plusYears(n);
        };
    }

    private Ticket fire(RecurringTicket r) {
        Ticket t = services.tickets().openFromTemplate(r);
        LocalDateTime base = r.getNextRunAt() == null ? now() : r.getNextRunAt();
        LocalDateTime next = next(base, r);
        while (!next.isAfter(now())) {
            next = next(next, r);
        }
        r.setLastRunAt(now());
        r.setNextRunAt(next);
        saveQuietly(r);
        return t;
    }

    int runDue() {
        int count = 0;
        for (RecurringTicket r : repo(RecurringTicket.class).find(Filter.and(Filter.isTrue("active"),
                Filter.le("nextRunAt", now())))) {
            tx(() -> fire(r));
            count++;
        }
        return count;
    }
}
