package com.hometusk.routines.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.time.DayOfWeek;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = RecurrenceRule.Daily.class, name = "DAILY"),
    @JsonSubTypes.Type(value = RecurrenceRule.Weekly.class, name = "WEEKLY"),
    @JsonSubTypes.Type(value = RecurrenceRule.Monthly.class, name = "MONTHLY"),
    @JsonSubTypes.Type(value = RecurrenceRule.EveryNDays.class, name = "EVERY_N_DAYS")
})
public sealed interface RecurrenceRule
        permits RecurrenceRule.Daily, RecurrenceRule.Weekly, RecurrenceRule.Monthly, RecurrenceRule.EveryNDays {

    record Daily() implements RecurrenceRule {}

    record Weekly(List<DayOfWeek> daysOfWeek) implements RecurrenceRule {}

    record Monthly(int dayOfMonth) implements RecurrenceRule {}

    record EveryNDays(int interval) implements RecurrenceRule {}
}
