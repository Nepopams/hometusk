package com.hometusk.routines.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.households.domain.Household;
import com.hometusk.households.service.HouseholdService;
import com.hometusk.households.service.ZoneService;
import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.RecurrenceRule;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.domain.RoutineStatus;
import com.hometusk.routines.dto.CreateRoutineRequest;
import com.hometusk.routines.dto.UpdateRoutineRequest;
import com.hometusk.routines.repository.RoutineRepository;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.users.domain.User;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserService;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoutineServiceTest {

    @Mock
    private RoutineRepository routineRepository;

    @Mock
    private HouseholdService householdService;

    @Mock
    private ZoneService zoneService;

    @Mock
    private UserService userService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private RecurrenceRuleParser recurrenceRuleParser;

    private RoutineService routineService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        routineService = new RoutineService(
                routineRepository,
                householdService,
                zoneService,
                userService,
                membershipService,
                objectMapper,
                recurrenceRuleParser);
    }

    @Test
    void createRoutine_withValidData_succeeds() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Test Household");
        User user = new User("ext", "test@example.com", "Test User");

        when(householdService.getById(householdId)).thenReturn(household);
        when(routineRepository.save(any(Routine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateRoutineRequest request = new CreateRoutineRequest(
                "Daily Task", "Desc", null, new RecurrenceRule.Daily(), AssignmentPolicy.ROUND_ROBIN, null, null);

        Routine routine = routineService.createRoutine(householdId, request, user);

        assertThat(routine.getTitle()).isEqualTo("Daily Task");
        assertThat(routine.getStatus()).isEqualTo(RoutineStatus.ACTIVE);
        assertThat(routine.getAssignmentPolicy()).isEqualTo(AssignmentPolicy.ROUND_ROBIN);
        assertThat(routine.getGenerationWindowDays()).isEqualTo(7);
    }

    @Test
    void createRoutine_weeklyWithoutDays_throwsBusinessViolation() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Test Household");
        User user = new User("ext", "test@example.com", "Test User");

        when(householdService.getById(householdId)).thenReturn(household);

        CreateRoutineRequest request = new CreateRoutineRequest(
                "Weekly Task",
                null,
                null,
                new RecurrenceRule.Weekly(List.of()),
                AssignmentPolicy.ROUND_ROBIN,
                null,
                null);

        assertThatThrownBy(() -> routineService.createRoutine(householdId, request, user))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
    }

    @Test
    void updateRoutine_partialUpdate_updatesOnlyNonNullFields() {
        UUID householdId = UUID.randomUUID();
        Routine routine = new Routine(
                new Household("Test Household"),
                "Old Title",
                "{\"type\":\"DAILY\"}",
                AssignmentPolicy.MANUAL,
                new User("ext", "test@example.com", "Test User"));
        routine.setDescription("Original");

        when(routineRepository.findByIdAndHousehold_Id(any(), eq(householdId))).thenReturn(Optional.of(routine));
        when(routineRepository.save(any(Routine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateRoutineRequest request = new UpdateRoutineRequest("New Title", null, null, null, null, null, null);

        Routine updated = routineService.updateRoutine(UUID.randomUUID(), householdId, request);

        assertThat(updated.getTitle()).isEqualTo("New Title");
        assertThat(updated.getDescription()).isEqualTo("Original");
    }

    @Test
    void deleteRoutine_setsStatusDeleted() {
        UUID householdId = UUID.randomUUID();
        Routine routine = new Routine(
                new Household("Test Household"),
                "Old Title",
                "{\"type\":\"DAILY\"}",
                AssignmentPolicy.MANUAL,
                new User("ext", "test@example.com", "Test User"));

        when(routineRepository.findByIdAndHousehold_Id(any(), eq(householdId))).thenReturn(Optional.of(routine));

        routineService.deleteRoutine(UUID.randomUUID(), householdId);

        assertThat(routine.getStatus()).isEqualTo(RoutineStatus.DELETED);
        verify(routineRepository).save(routine);
    }

    @Test
    void listRoutines_defaultExcludesDeleted() {
        UUID householdId = UUID.randomUUID();
        ArgumentCaptor<List<RoutineStatus>> statusCaptor = ArgumentCaptor.forClass(List.class);

        routineService.listRoutines(householdId, null, null);

        verify(routineRepository)
                .findByHousehold_IdAndStatusInOrderByCreatedAtDesc(eq(householdId), statusCaptor.capture());

        assertThat(statusCaptor.getValue()).containsExactlyInAnyOrder(RoutineStatus.ACTIVE, RoutineStatus.PAUSED);
    }

    @Test
    void recurrenceRule_serialization_roundTrip() throws Exception {
        RecurrenceRule rule = new RecurrenceRule.Weekly(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        String json = objectMapper.writeValueAsString(rule);
        RecurrenceRule parsed = objectMapper.readValue(json, RecurrenceRule.class);

        assertThat(json).contains("\"type\":\"WEEKLY\"");
        assertThat(parsed).isInstanceOf(RecurrenceRule.Weekly.class);
        RecurrenceRule.Weekly weekly = (RecurrenceRule.Weekly) parsed;
        assertThat(weekly.daysOfWeek()).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
    }

    @Test
    void createRoutine_fixedAssigneeNotMember_throwsBusinessViolation() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Test Household");
        User user = new User("ext", "test@example.com", "Test User");
        UUID assigneeId = UUID.randomUUID();

        when(householdService.getById(householdId)).thenReturn(household);
        when(membershipService.isMember(assigneeId, householdId)).thenReturn(false);

        CreateRoutineRequest request = new CreateRoutineRequest(
                "Fixed Task", null, null, new RecurrenceRule.Daily(), AssignmentPolicy.FIXED, assigneeId, null);

        assertThatThrownBy(() -> routineService.createRoutine(householdId, request, user))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
    }

    @Test
    void createRoutine_invalidZone_throwsBusinessViolation() {
        UUID householdId = UUID.randomUUID();
        Household household = new Household("Test Household");
        User user = new User("ext", "test@example.com", "Test User");
        UUID zoneId = UUID.randomUUID();

        when(householdService.getById(householdId)).thenReturn(household);
        when(zoneService.findByIdAndHouseholdId(zoneId, householdId)).thenReturn(Optional.empty());

        CreateRoutineRequest request = new CreateRoutineRequest(
                "Zone Task", null, zoneId, new RecurrenceRule.Daily(), AssignmentPolicy.MANUAL, null, null);

        assertThatThrownBy(() -> routineService.createRoutine(householdId, request, user))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
    }

    @Test
    void updateRoutine_fixedAssigneeForNonFixedPolicy_throwsBusinessViolation() {
        UUID householdId = UUID.randomUUID();
        Routine routine = new Routine(
                new Household("Test Household"),
                "Old Title",
                "{\"type\":\"DAILY\"}",
                AssignmentPolicy.MANUAL,
                new User("ext", "test@example.com", "Test User"));

        when(routineRepository.findByIdAndHousehold_Id(any(), eq(householdId))).thenReturn(Optional.of(routine));

        UpdateRoutineRequest request = new UpdateRoutineRequest(null, null, null, null, null, UUID.randomUUID(), null);

        assertThatThrownBy(() -> routineService.updateRoutine(UUID.randomUUID(), householdId, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
    }
}
