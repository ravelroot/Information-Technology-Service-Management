package io.pyhoma.domain.service;

import io.pyhoma.domain.itil.BusinessCalendar;
import io.pyhoma.domain.itil.SlaState;
import io.pyhoma.domain.model.core.Module;
import io.pyhoma.domain.model.itil.Holiday;
import io.pyhoma.domain.model.itil.Sla;
import io.pyhoma.domain.model.itil.SlaType;
import io.pyhoma.domain.model.itil.Ticket;
import io.pyhoma.domain.query.Filter;
import io.pyhoma.domain.query.Query;
import io.pyhoma.domain.query.Sort;
import io.pyhoma.domain.security.Right;
import io.pyhoma.domain.support.Texts;
import io.pyhoma.domain.support.Validator;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SlaService extends AbstractService {

    private volatile BusinessCalendar calendar;

    public SlaService(Services services) {
        super(services);
    }

    public List<Sla> all() {
        return repo(Sla.class).find(Query.where(security().orgFilter()).orderBy(Sort.asc("slaType"), Sort.asc("priority"),
                Sort.asc("name")));
    }

    public Sla get(Long id) {
        return repo(Sla.class).getById(id);
    }

    public Sla save(Sla sla) {
        require(Module.SLA, sla.isNew() ? Right.CREATE : Right.UPDATE);
        sla.setName(Texts.clean(sla.getName()));
        Validator.create().required("name", sla.getName()).required("slaType", sla.getSlaType())
                .required("orgUnitId", sla.getOrgUnitId())
                .check(sla.getDurationMinutes() != null && sla.getDurationMinutes() > 0, "durationMinutes", "validation.positive")
                .range("priority", sla.getPriority(), 1, 6).throwIfInvalid();
        return tx(() -> persist(sla));
    }

    public void delete(Long id) {
        require(Module.SLA, Right.DELETE);
        txRun(() -> purge(get(id)));
    }

    public List<Holiday> holidays() {
        return repo(Holiday.class).find(Query.all().orderBy(Sort.asc("holidayDate")));
    }

    public Holiday saveHoliday(Holiday holiday) {
        require(Module.SLA, holiday.isNew() ? Right.CREATE : Right.UPDATE);
        holiday.setName(Texts.clean(holiday.getName()));
        Validator.create().required("name", holiday.getName()).required("holidayDate", holiday.getHolidayDate()).throwIfInvalid();
        Holiday saved = tx(() -> persist(holiday));
        calendar = null;
        return saved;
    }

    public void deleteHoliday(Long id) {
        require(Module.SLA, Right.DELETE);
        txRun(() -> purge(repo(Holiday.class).getById(id)));
        calendar = null;
    }

    public void saveCalendar(Set<DayOfWeek> days, LocalTime start, LocalTime end) {
        require(Module.SETTINGS, Right.UPDATE);
        Validator.create().check(!days.isEmpty(), "workDays", "validation.required")
                .check(end.isAfter(start), "dayEnd", "validation.range").throwIfInvalid();
        services.settings().set(SettingKeys.CALENDAR_WORK_DAYS,
                days.stream().sorted().map(DayOfWeek::name).collect(Collectors.joining(",")));
        services.settings().set(SettingKeys.CALENDAR_DAY_START, start.toString());
        services.settings().set(SettingKeys.CALENDAR_DAY_END, end.toString());
        calendar = null;
    }

    public BusinessCalendar calendar() {
        BusinessCalendar c = calendar;
        if (c == null) {
            c = loadCalendar();
            calendar = c;
        }
        return c;
    }

    public void invalidateCalendar() {
        calendar = null;
    }

    private BusinessCalendar loadCalendar() {
        SettingsService s = services.settings();
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String d : s.get(SettingKeys.CALENDAR_WORK_DAYS, "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY").split(",")) {
            if (!d.isBlank()) {
                days.add(DayOfWeek.valueOf(d.trim()));
            }
        }
        LocalTime start = LocalTime.parse(s.get(SettingKeys.CALENDAR_DAY_START, "08:00"));
        LocalTime end = LocalTime.parse(s.get(SettingKeys.CALENDAR_DAY_END, "18:00"));
        Set<LocalDate> fixed = new HashSet<>();
        Set<MonthDay> yearly = new HashSet<>();
        for (Holiday h : repo(Holiday.class).find(Filter.ALL)) {
            if (h.isRecurring()) {
                yearly.add(MonthDay.from(h.getHolidayDate()));
            } else {
                fixed.add(h.getHolidayDate());
            }
        }
        return new BusinessCalendar(days, start, end, fixed, yearly);
    }

    public Optional<Sla> resolve(Ticket ticket, SlaType type) {
        List<Long> chain = services.orgUnits().selfAndAncestors(ticket.getOrgUnitId());
        List<Sla> candidates = repo(Sla.class).find(Filter.and(Filter.eq("slaType", type), Filter.isTrue("active"),
                Filter.in("orgUnitId", chain)));
        for (Long unit : chain) {
            Optional<Sla> exact = candidates.stream().filter(s -> unit.equals(s.getOrgUnitId())
                    && Objects.equals(s.getPriority(), ticket.getPriority())).findFirst();
            if (exact.isPresent()) {
                return exact;
            }
            Optional<Sla> generic = candidates.stream().filter(s -> unit.equals(s.getOrgUnitId()) && s.getPriority() == null)
                    .findFirst();
            if (generic.isPresent()) {
                return generic;
            }
        }
        return Optional.empty();
    }

    public void apply(Ticket ticket) {
        Sla tto = ticket.getSlaTtoId() != null ? repo(Sla.class).findById(ticket.getSlaTtoId()).orElse(null)
                : resolve(ticket, SlaType.TTO).orElse(null);
        Sla ttr = ticket.getSlaTtrId() != null ? repo(Sla.class).findById(ticket.getSlaTtrId()).orElse(null)
                : resolve(ticket, SlaType.TTR).orElse(null);
        LocalDateTime opened = ticket.getOpenedAt();
        if (tto != null) {
            ticket.setSlaTtoId(tto.getId());
            ticket.setTtoDueAt(due(opened, tto));
        }
        if (ttr != null) {
            ticket.setSlaTtrId(ttr.getId());
            ticket.setDueAt(due(opened, ttr).plusMinutes(0));
            if (ticket.getWaitingMinutes() != null && ticket.getWaitingMinutes() > 0) {
                ticket.setDueAt(shift(ticket.getDueAt(), ticket.getWaitingMinutes(), ttr.isBusinessHours()));
            }
        }
    }

    public LocalDateTime due(LocalDateTime start, Sla sla) {
        return sla.isBusinessHours() ? calendar().addBusinessMinutes(start, sla.getDurationMinutes())
                : start.plusMinutes(sla.getDurationMinutes());
    }

    private LocalDateTime shift(LocalDateTime due, long minutes, boolean business) {
        return business ? calendar().addBusinessMinutes(due, minutes) : due.plusMinutes(minutes);
    }

    void pause(Ticket ticket) {
        if (ticket.getWaitingSince() == null) {
            ticket.setWaitingSince(now());
        }
    }

    void resume(Ticket ticket) {
        if (ticket.getWaitingSince() == null) {
            return;
        }
        boolean business = ticket.getSlaTtrId() == null || repo(Sla.class).findById(ticket.getSlaTtrId())
                .map(Sla::isBusinessHours).orElse(true);
        long waited = business ? calendar().businessMinutesBetween(ticket.getWaitingSince(), now())
                : Duration.between(ticket.getWaitingSince(), now()).toMinutes();
        ticket.setWaitingMinutes(ticket.getWaitingMinutes() + (int) waited);
        if (ticket.getDueAt() != null && waited > 0) {
            ticket.setDueAt(shift(ticket.getDueAt(), waited, business));
        }
        ticket.setWaitingSince(null);
    }

    public SlaState ttoState(Ticket t) {
        return state(t.getOpenedAt(), t.getTtoDueAt(), t.getTakenAt(), false);
    }

    public SlaState ttrState(Ticket t) {
        return state(t.getOpenedAt(), t.getDueAt(), t.getSolvedAt(), t.getWaitingSince() != null);
    }

    SlaState state(LocalDateTime opened, LocalDateTime due, LocalDateTime done, boolean paused) {
        if (due == null) {
            return SlaState.NONE;
        }
        if (done != null) {
            return done.isAfter(due) ? SlaState.MISSED : SlaState.MET;
        }
        LocalDateTime now = now();
        if (now.isAfter(due)) {
            return paused ? SlaState.AT_RISK : SlaState.BREACHED;
        }
        long total = Math.max(1, Duration.between(opened, due).toMinutes());
        long left = Duration.between(now, due).toMinutes();
        int warn = services.settings().getInt(SettingKeys.SLA_WARNING_PERCENT, 25);
        return left * 100 / total <= warn ? SlaState.AT_RISK : SlaState.ON_TIME;
    }
}
