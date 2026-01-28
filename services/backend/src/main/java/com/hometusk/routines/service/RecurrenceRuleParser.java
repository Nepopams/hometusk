package com.hometusk.routines.service;

import com.hometusk.routines.domain.InvalidRecurrenceRuleException;
import com.hometusk.routines.domain.RecurrenceRule;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class RecurrenceRuleParser {

    public List<LocalDate> getOccurrencesInRange(RecurrenceRule rule, LocalDate fromDateInclusive, int count) {
        validate(rule);
        if (count <= 0) {
            return List.of();
        }

        return switch (rule) {
            case RecurrenceRule.Daily daily -> dailyOccurrences(fromDateInclusive, count);
            case RecurrenceRule.Weekly weekly -> weeklyOccurrences(weekly, fromDateInclusive, count);
            case RecurrenceRule.Monthly monthly -> monthlyOccurrences(monthly, fromDateInclusive, count);
            case RecurrenceRule.EveryNDays everyNDays -> everyNDaysOccurrences(everyNDays, fromDateInclusive, count);
        };
    }

    public LocalDate getNextOccurrence(RecurrenceRule rule, LocalDate afterDateExclusive) {
        validate(rule);

        return switch (rule) {
            case RecurrenceRule.Daily daily -> afterDateExclusive.plusDays(1);
            case RecurrenceRule.Weekly weekly -> nextWeeklyOccurrence(weekly, afterDateExclusive);
            case RecurrenceRule.Monthly monthly -> nextMonthlyOccurrence(monthly, afterDateExclusive);
            case RecurrenceRule.EveryNDays everyNDays -> afterDateExclusive.plusDays(everyNDays.interval());
        };
    }

    private List<LocalDate> dailyOccurrences(LocalDate start, int count) {
        List<LocalDate> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            results.add(start.plusDays(i));
        }
        return results;
    }

    private List<LocalDate> weeklyOccurrences(RecurrenceRule.Weekly weekly, LocalDate start, int count) {
        Set<java.time.DayOfWeek> days = EnumSet.copyOf(weekly.daysOfWeek());
        List<LocalDate> results = new ArrayList<>(count);
        LocalDate cursor = start;
        while (results.size() < count) {
            if (days.contains(cursor.getDayOfWeek())) {
                results.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return results;
    }

    private List<LocalDate> monthlyOccurrences(RecurrenceRule.Monthly monthly, LocalDate start, int count) {
        List<LocalDate> results = new ArrayList<>(count);
        YearMonth currentMonth = YearMonth.from(start);
        LocalDate candidate = clampDay(currentMonth, monthly.dayOfMonth());
        if (candidate.isBefore(start)) {
            currentMonth = currentMonth.plusMonths(1);
            candidate = clampDay(currentMonth, monthly.dayOfMonth());
        }

        while (results.size() < count) {
            results.add(candidate);
            currentMonth = currentMonth.plusMonths(1);
            candidate = clampDay(currentMonth, monthly.dayOfMonth());
        }
        return results;
    }

    private List<LocalDate> everyNDaysOccurrences(RecurrenceRule.EveryNDays everyNDays, LocalDate start, int count) {
        List<LocalDate> results = new ArrayList<>(count);
        long interval = everyNDays.interval();
        for (int i = 0; i < count; i++) {
            results.add(start.plusDays(interval * i));
        }
        return results;
    }

    private LocalDate nextWeeklyOccurrence(RecurrenceRule.Weekly weekly, LocalDate afterDateExclusive) {
        Set<java.time.DayOfWeek> days = EnumSet.copyOf(weekly.daysOfWeek());
        LocalDate cursor = afterDateExclusive.plusDays(1);
        while (true) {
            if (days.contains(cursor.getDayOfWeek())) {
                return cursor;
            }
            cursor = cursor.plusDays(1);
        }
    }

    private LocalDate nextMonthlyOccurrence(RecurrenceRule.Monthly monthly, LocalDate afterDateExclusive) {
        YearMonth currentMonth = YearMonth.from(afterDateExclusive);
        LocalDate candidate = clampDay(currentMonth, monthly.dayOfMonth());
        if (candidate.isAfter(afterDateExclusive)) {
            return candidate;
        }
        YearMonth nextMonth = currentMonth.plusMonths(1);
        return clampDay(nextMonth, monthly.dayOfMonth());
    }

    private LocalDate clampDay(YearMonth month, int dayOfMonth) {
        int clamped = Math.min(dayOfMonth, month.lengthOfMonth());
        return month.atDay(clamped);
    }

    private void validate(RecurrenceRule rule) {
        if (rule == null) {
            throw new InvalidRecurrenceRuleException("Recurrence rule is required");
        }

        if (rule instanceof RecurrenceRule.Weekly weekly) {
            if (weekly.daysOfWeek() == null || weekly.daysOfWeek().isEmpty()) {
                throw new InvalidRecurrenceRuleException("daysOfWeek is required for WEEKLY type");
            }
        } else if (rule instanceof RecurrenceRule.Monthly monthly) {
            int day = monthly.dayOfMonth();
            if (day < 1 || day > 31) {
                throw new InvalidRecurrenceRuleException("dayOfMonth must be between 1 and 31");
            }
        } else if (rule instanceof RecurrenceRule.EveryNDays everyNDays) {
            if (everyNDays.interval() <= 0) {
                throw new InvalidRecurrenceRuleException("interval must be greater than 0");
            }
        }
    }
}
