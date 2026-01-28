package com.hometusk.routines.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hometusk.routines.domain.InvalidRecurrenceRuleException;
import com.hometusk.routines.domain.RecurrenceRule;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecurrenceRuleParserTest {

    private RecurrenceRuleParser parser;

    @BeforeEach
    void setUp() {
        parser = new RecurrenceRuleParser();
    }

    @Test
    void daily_returnsConsecutiveDaysFromInclusiveStart() {
        RecurrenceRule rule = new RecurrenceRule.Daily();
        LocalDate from = LocalDate.of(2026, 1, 28);

        List<LocalDate> result = parser.getOccurrencesInRange(rule, from, 3);

        assertThat(result)
                .containsExactly(LocalDate.of(2026, 1, 28), LocalDate.of(2026, 1, 29), LocalDate.of(2026, 1, 30));
    }

    @Test
    void weekly_singleDay_findsNextOccurrenceExclusive() {
        RecurrenceRule rule = new RecurrenceRule.Weekly(List.of(DayOfWeek.SATURDAY));
        LocalDate after = LocalDate.of(2026, 1, 28);

        LocalDate next = parser.getNextOccurrence(rule, after);

        assertThat(next).isEqualTo(LocalDate.of(2026, 1, 31));
    }

    @Test
    void weekly_multipleDays_returnsCorrectSequenceFromInclusiveStart() {
        RecurrenceRule rule = new RecurrenceRule.Weekly(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
        LocalDate from = LocalDate.of(2026, 1, 28);

        List<LocalDate> result = parser.getOccurrencesInRange(rule, from, 4);

        assertThat(result)
                .containsExactly(
                        LocalDate.of(2026, 1, 30),
                        LocalDate.of(2026, 2, 2),
                        LocalDate.of(2026, 2, 6),
                        LocalDate.of(2026, 2, 9));
    }

    @Test
    void monthly_normalDay_findsNextOccurrenceExclusive() {
        RecurrenceRule rule = new RecurrenceRule.Monthly(15);
        LocalDate after = LocalDate.of(2026, 1, 28);

        LocalDate next = parser.getNextOccurrence(rule, after);

        assertThat(next).isEqualTo(LocalDate.of(2026, 2, 15));
    }

    @Test
    void monthly_dayExceedsMonthLength_clampsToLastDay() {
        RecurrenceRule rule = new RecurrenceRule.Monthly(31);
        LocalDate after = LocalDate.of(2026, 2, 1);

        LocalDate next = parser.getNextOccurrence(rule, after);

        assertThat(next).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void everyNDays_spacesCorrectlyFromInclusiveStart() {
        RecurrenceRule rule = new RecurrenceRule.EveryNDays(3);
        LocalDate from = LocalDate.of(2026, 1, 28);

        List<LocalDate> result = parser.getOccurrencesInRange(rule, from, 4);

        assertThat(result)
                .containsExactly(
                        LocalDate.of(2026, 1, 28),
                        LocalDate.of(2026, 1, 31),
                        LocalDate.of(2026, 2, 3),
                        LocalDate.of(2026, 2, 6));
    }

    @Test
    void validation_weeklyWithoutDaysOfWeek_throws() {
        RecurrenceRule ruleWithNull = new RecurrenceRule.Weekly(null);
        RecurrenceRule ruleWithEmpty = new RecurrenceRule.Weekly(List.of());

        assertThatThrownBy(() -> parser.getNextOccurrence(ruleWithNull, LocalDate.now()))
                .isInstanceOf(InvalidRecurrenceRuleException.class);
        assertThatThrownBy(() -> parser.getNextOccurrence(ruleWithEmpty, LocalDate.now()))
                .isInstanceOf(InvalidRecurrenceRuleException.class);
    }

    @Test
    void validation_monthlyWithoutDayOfMonth_throws() {
        RecurrenceRule ruleZero = new RecurrenceRule.Monthly(0);
        RecurrenceRule ruleTooHigh = new RecurrenceRule.Monthly(32);

        assertThatThrownBy(() -> parser.getNextOccurrence(ruleZero, LocalDate.now()))
                .isInstanceOf(InvalidRecurrenceRuleException.class);
        assertThatThrownBy(() -> parser.getNextOccurrence(ruleTooHigh, LocalDate.now()))
                .isInstanceOf(InvalidRecurrenceRuleException.class);
    }

    @Test
    void validation_everyNDaysWithZeroInterval_throws() {
        RecurrenceRule ruleZero = new RecurrenceRule.EveryNDays(0);
        RecurrenceRule ruleNegative = new RecurrenceRule.EveryNDays(-2);

        assertThatThrownBy(() -> parser.getNextOccurrence(ruleZero, LocalDate.now()))
                .isInstanceOf(InvalidRecurrenceRuleException.class);
        assertThatThrownBy(() -> parser.getNextOccurrence(ruleNegative, LocalDate.now()))
                .isInstanceOf(InvalidRecurrenceRuleException.class);
    }

    @Test
    void getNextOccurrence_neverReturnsAfterDateItself() {
        RecurrenceRule rule = new RecurrenceRule.Daily();
        LocalDate after = LocalDate.of(2026, 1, 28);

        LocalDate next = parser.getNextOccurrence(rule, after);

        assertThat(next).isEqualTo(LocalDate.of(2026, 1, 29));
    }

    @Test
    void leapYear_february29Handled() {
        RecurrenceRule rule = new RecurrenceRule.Monthly(29);
        LocalDate after = LocalDate.of(2024, 2, 1);

        LocalDate next = parser.getNextOccurrence(rule, after);

        assertThat(next).isEqualTo(LocalDate.of(2024, 2, 29));
    }

    @Test
    void yearBoundary_crossesCorrectly() {
        RecurrenceRule rule = new RecurrenceRule.Daily();
        LocalDate from = LocalDate.of(2026, 12, 30);

        List<LocalDate> result = parser.getOccurrencesInRange(rule, from, 5);

        assertThat(result)
                .containsExactly(
                        LocalDate.of(2026, 12, 30),
                        LocalDate.of(2026, 12, 31),
                        LocalDate.of(2027, 1, 1),
                        LocalDate.of(2027, 1, 2),
                        LocalDate.of(2027, 1, 3));
    }
}
