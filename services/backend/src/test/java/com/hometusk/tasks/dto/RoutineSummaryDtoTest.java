package com.hometusk.tasks.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.hometusk.households.domain.Household;
import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.domain.RoutineStatus;
import com.hometusk.users.domain.User;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RoutineSummaryDtoTest {

    @Test
    void from_routine_mapsCorrectly() {
        Routine routine = routineWithStatus(RoutineStatus.ACTIVE);

        RoutineSummaryDto dto = RoutineSummaryDto.from(routine);

        assertThat(dto.id()).isEqualTo(routine.getId());
        assertThat(dto.title()).isEqualTo(routine.getTitle());
        assertThat(dto.status()).isEqualTo(RoutineStatus.ACTIVE.name());
    }

    @Test
    void from_null_returnsNull() {
        assertThat(RoutineSummaryDto.from(null)).isNull();
    }

    @Test
    void from_deletedRoutine_hasDeletedStatus() {
        Routine routine = routineWithStatus(RoutineStatus.DELETED);

        RoutineSummaryDto dto = RoutineSummaryDto.from(routine);

        assertThat(dto.status()).isEqualTo(RoutineStatus.DELETED.name());
    }

    private Routine routineWithStatus(RoutineStatus status) {
        Household household = new Household("Household");
        setField(household, "id", UUID.randomUUID());
        User creator = new User("ext-" + UUID.randomUUID(), "test@example.com", "Creator");
        setField(creator, "id", UUID.randomUUID());
        Routine routine = new Routine(household, "Routine", "{\"type\":\"DAILY\"}", AssignmentPolicy.MANUAL, creator);
        routine.setStatus(status);
        return routine;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field " + fieldName, e);
        }
    }
}
