package io.pyhoma.domain.itil;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.EnumSet;
import java.util.Set;

public final class BusinessCalendar {

    private static final int MAX_DAYS = 3 * 366;

    private final Set<DayOfWeek> workDays;
    private final LocalTime dayStart;
    private final LocalTime dayEnd;
    private final Set<LocalDate> holidays;
    private final Set<MonthDay> yearlyHolidays;

    public BusinessCalendar(Set<DayOfWeek> workDays, LocalTime dayStart, LocalTime dayEnd,
                            Set<LocalDate> holidays, Set<MonthDay> yearlyHolidays) {
        if (!dayEnd.isAfter(dayStart)) {
            throw new IllegalArgumentException("dayEnd must be after dayStart");
        }
        this.workDays = workDays.isEmpty() ? EnumSet.allOf(DayOfWeek.class) : EnumSet.copyOf(workDays);
        this.dayStart = dayStart;
        this.dayEnd = dayEnd;
        this.holidays = Set.copyOf(holidays);
        this.yearlyHolidays = Set.copyOf(yearlyHolidays);
    }

    public static BusinessCalendar standard() {
        return new BusinessCalendar(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), LocalTime.of(8, 0),
                LocalTime.of(18, 0), Set.of(), Set.of());
    }

    public boolean isWorkingDay(LocalDate date) {
        return workDays.contains(date.getDayOfWeek()) && !holidays.contains(date)
                && !yearlyHolidays.contains(MonthDay.from(date));
    }

    public long minutesPerDay() {
        return Duration.between(dayStart, dayEnd).toMinutes();
    }

    public LocalDateTime nextWorkingInstant(LocalDateTime t) {
        LocalDateTime cursor = t;
        for (int i = 0; i < MAX_DAYS; i++) {
            LocalDate d = cursor.toLocalDate();
            if (isWorkingDay(d)) {
                if (cursor.toLocalTime().isBefore(dayStart)) {
                    return d.atTime(dayStart);
                }
                if (cursor.toLocalTime().isBefore(dayEnd)) {
                    return cursor;
                }
            }
            cursor = d.plusDays(1).atTime(dayStart);
        }
        throw new IllegalStateException("No working day found in the calendar");
    }

    public LocalDateTime addBusinessMinutes(LocalDateTime start, long minutes) {
        if (minutes <= 0) {
            return start;
        }
        LocalDateTime cursor = nextWorkingInstant(start);
        long remaining = minutes;
        for (int i = 0; i < MAX_DAYS * 2; i++) {
            long available = Duration.between(cursor.toLocalTime(), dayEnd).toMinutes();
            if (remaining <= available) {
                return cursor.plusMinutes(remaining);
            }
            remaining -= available;
            cursor = nextWorkingInstant(cursor.toLocalDate().plusDays(1).atTime(dayStart));
        }
        throw new IllegalStateException("Duration too long for the calendar");
    }

    public long businessMinutesBetween(LocalDateTime from, LocalDateTime to) {
        if (!to.isAfter(from)) {
            return 0;
        }
        long total = 0;
        LocalDate day = from.toLocalDate();
        LocalDate last = to.toLocalDate();
        int guard = 0;
        while (!day.isAfter(last) && guard++ < MAX_DAYS * 10) {
            if (isWorkingDay(day)) {
                LocalDateTime open = day.atTime(dayStart);
                LocalDateTime close = day.atTime(dayEnd);
                LocalDateTime a = from.isAfter(open) ? from : open;
                LocalDateTime b = to.isBefore(close) ? to : close;
                if (b.isAfter(a)) {
                    total += Duration.between(a, b).toMinutes();
                }
            }
            day = day.plusDays(1);
        }
        return total;
    }

    public Set<DayOfWeek> workDays() {
        return EnumSet.copyOf(workDays);
    }

    public LocalTime dayStart() {
        return dayStart;
    }

    public LocalTime dayEnd() {
        return dayEnd;
    }
}
