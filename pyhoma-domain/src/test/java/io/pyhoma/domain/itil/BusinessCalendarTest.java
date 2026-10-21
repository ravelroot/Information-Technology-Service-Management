package io.pyhoma.domain.itil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BusinessCalendarTest {

    private final BusinessCalendar calendar = new BusinessCalendar(EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            LocalTime.of(8, 0), LocalTime.of(18, 0), Set.of(LocalDate.of(2026, 3, 4)), Set.of(MonthDay.of(12, 25)));

    @Test
    void addsMinutesInsideTheSameDay() {
        assertEquals(LocalDateTime.of(2026, 3, 2, 11, 0), calendar.addBusinessMinutes(LocalDateTime.of(2026, 3, 2, 9, 0), 120));
    }

    @Test
    void rollsOverToTheNextWorkingDay() {
        assertEquals(LocalDateTime.of(2026, 3, 3, 9, 0), calendar.addBusinessMinutes(LocalDateTime.of(2026, 3, 2, 17, 0), 120));
    }

    @Test
    void skipsWeekendsAndHolidays() {
        assertEquals(LocalDateTime.of(2026, 3, 5, 9, 0), calendar.addBusinessMinutes(LocalDateTime.of(2026, 3, 3, 17, 0), 120));
        assertEquals(LocalDateTime.of(2026, 3, 9, 8, 30), calendar.addBusinessMinutes(LocalDateTime.of(2026, 3, 6, 17, 30), 60));
    }

    @Test
    void startOutsideOfficeHoursBeginsAtOpening() {
        assertEquals(LocalDateTime.of(2026, 3, 2, 9, 0), calendar.addBusinessMinutes(LocalDateTime.of(2026, 3, 2, 6, 0), 60));
        assertEquals(LocalDateTime.of(2026, 3, 9, 9, 0), calendar.addBusinessMinutes(LocalDateTime.of(2026, 3, 7, 12, 0), 60));
    }

    @Test
    void countsBusinessMinutesBetweenInstants() {
        assertEquals(600, calendar.businessMinutesBetween(LocalDateTime.of(2026, 3, 2, 0, 0), LocalDateTime.of(2026, 3, 3, 0, 0)));
        assertEquals(120, calendar.businessMinutesBetween(LocalDateTime.of(2026, 3, 6, 17, 0), LocalDateTime.of(2026, 3, 9, 9, 0)));
        assertEquals(0, calendar.businessMinutesBetween(LocalDateTime.of(2026, 3, 9, 9, 0), LocalDateTime.of(2026, 3, 6, 17, 0)));
    }

    @Test
    void recurringHolidayAppliesEveryYear() {
        assertFalse(calendar.isWorkingDay(LocalDate.of(2030, 12, 25)));
        assertTrue(calendar.isWorkingDay(LocalDate.of(2030, 12, 24)));
    }
}
