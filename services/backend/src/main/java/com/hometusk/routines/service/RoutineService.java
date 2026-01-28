package com.hometusk.routines.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.Zone;
import com.hometusk.households.service.HouseholdService;
import com.hometusk.households.service.ZoneService;
import com.hometusk.routines.domain.AssignmentPolicy;
import com.hometusk.routines.domain.RecurrenceRule;
import com.hometusk.routines.domain.Routine;
import com.hometusk.routines.domain.RoutineStatus;
import com.hometusk.routines.dto.CreateRoutineRequest;
import com.hometusk.routines.dto.UpcomingInstanceDto;
import com.hometusk.routines.dto.UpcomingInstancesResponse;
import com.hometusk.routines.dto.UpdateRoutineRequest;
import com.hometusk.routines.dto.UserSummaryDto;
import com.hometusk.routines.repository.RoutineRepository;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.NotFoundException;
import com.hometusk.users.domain.User;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserService;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoutineService {

    private static final Logger log = LoggerFactory.getLogger(RoutineService.class);

    private final RoutineRepository routineRepository;
    private final HouseholdService householdService;
    private final ZoneService zoneService;
    private final UserService userService;
    private final MembershipService membershipService;
    private final ObjectMapper objectMapper;
    private final RecurrenceRuleParser recurrenceRuleParser;

    public RoutineService(
            RoutineRepository routineRepository,
            HouseholdService householdService,
            ZoneService zoneService,
            UserService userService,
            MembershipService membershipService,
            ObjectMapper objectMapper,
            RecurrenceRuleParser recurrenceRuleParser) {
        this.routineRepository = routineRepository;
        this.householdService = householdService;
        this.zoneService = zoneService;
        this.userService = userService;
        this.membershipService = membershipService;
        this.objectMapper = objectMapper;
        this.recurrenceRuleParser = recurrenceRuleParser;
    }

    @Transactional(readOnly = true)
    public List<Routine> listRoutines(UUID householdId, RoutineStatus status, AssignmentPolicy assignmentPolicy) {
        List<RoutineStatus> statuses =
                status != null ? List.of(status) : List.of(RoutineStatus.ACTIVE, RoutineStatus.PAUSED);

        if (assignmentPolicy != null) {
            return routineRepository.findByHousehold_IdAndStatusInAndAssignmentPolicyOrderByCreatedAtDesc(
                    householdId, statuses, assignmentPolicy);
        }

        return routineRepository.findByHousehold_IdAndStatusInOrderByCreatedAtDesc(householdId, statuses);
    }

    @Transactional(readOnly = true)
    public Routine getRoutine(UUID routineId, UUID householdId) {
        return routineRepository
                .findByIdAndHousehold_Id(routineId, householdId)
                .orElseThrow(
                        () -> new NotFoundException(ErrorCode.ROUTINE_NOT_FOUND, "Routine not found: " + routineId));
    }

    @Transactional
    public Routine createRoutine(UUID householdId, CreateRoutineRequest request, User createdBy) {
        Household household = householdService.getById(householdId);

        validateRecurrenceRule(request.recurrenceRule());
        String ruleJson = serializeRecurrenceRule(request.recurrenceRule());

        Routine routine = new Routine(household, request.title(), ruleJson, request.assignmentPolicy(), createdBy);

        if (request.description() != null) {
            routine.setDescription(request.description());
        }

        if (request.zoneId() != null) {
            routine.setZone(resolveZone(request.zoneId(), householdId));
        }

        if (request.generationWindowDays() != null) {
            routine.setGenerationWindowDays(request.generationWindowDays());
        }

        if (request.assignmentPolicy() != AssignmentPolicy.FIXED && request.fixedAssigneeId() != null) {
            throw businessViolation(
                    "ASSIGNEE_MUST_BE_MEMBER", "Fixed assignee can only be set when assignmentPolicy is FIXED");
        }

        applyAssignmentPolicy(routine, request.assignmentPolicy(), request.fixedAssigneeId(), householdId, true, false);

        Routine saved = routineRepository.save(routine);
        log.info("Routine created: id={}, householdId={}", saved.getId(), householdId);
        return saved;
    }

    @Transactional
    public Routine updateRoutine(UUID routineId, UUID householdId, UpdateRoutineRequest request) {
        Routine routine = getRoutine(routineId, householdId);

        AssignmentPolicy currentPolicy = routine.getAssignmentPolicy();
        AssignmentPolicy effectivePolicy =
                request.assignmentPolicy() != null ? request.assignmentPolicy() : currentPolicy;
        boolean policyChanged = request.assignmentPolicy() != null && request.assignmentPolicy() != currentPolicy;

        if (request.title() != null) {
            routine.setTitle(request.title());
        }

        if (request.description() != null) {
            routine.setDescription(request.description());
        }

        if (request.zoneId() != null) {
            routine.setZone(resolveZone(request.zoneId(), householdId));
        }

        if (request.recurrenceRule() != null) {
            validateRecurrenceRule(request.recurrenceRule());
            routine.setRecurrenceRuleJson(serializeRecurrenceRule(request.recurrenceRule()));
        }

        if (request.generationWindowDays() != null) {
            routine.setGenerationWindowDays(request.generationWindowDays());
        }

        if (request.assignmentPolicy() != null) {
            routine.setAssignmentPolicy(effectivePolicy);
        }

        if (request.fixedAssigneeId() != null && effectivePolicy != AssignmentPolicy.FIXED) {
            throw businessViolation(
                    "ASSIGNEE_MUST_BE_MEMBER", "Fixed assignee can only be set when assignmentPolicy is FIXED");
        }

        applyAssignmentPolicy(routine, effectivePolicy, request.fixedAssigneeId(), householdId, false, policyChanged);

        Routine saved = routineRepository.save(routine);
        log.info("Routine updated: id={}, householdId={}", saved.getId(), householdId);
        return saved;
    }

    @Transactional
    public void deleteRoutine(UUID routineId, UUID householdId) {
        Routine routine = getRoutine(routineId, householdId);
        routine.softDelete();
        routineRepository.save(routine);
        log.info("Routine soft-deleted: id={}, householdId={}", routineId, householdId);
    }

    @Transactional
    public Routine pauseRoutine(UUID routineId, UUID householdId) {
        Routine routine = getRoutine(routineId, householdId);

        if (routine.getStatus() == RoutineStatus.DELETED) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Cannot pause a deleted routine",
                    List.of(new BusinessException.Violation("ROUTINE_DELETED", "Routine is deleted")));
        }

        if (routine.getStatus() != RoutineStatus.PAUSED) {
            routine.setStatus(RoutineStatus.PAUSED);
            routine.setPausedAt(Instant.now());
            routine = routineRepository.save(routine);
            log.info("Routine paused: id={}, householdId={}", routineId, householdId);
        }

        return routine;
    }

    @Transactional
    public Routine resumeRoutine(UUID routineId, UUID householdId) {
        Routine routine = getRoutine(routineId, householdId);

        if (routine.getStatus() == RoutineStatus.DELETED) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Cannot resume a deleted routine",
                    List.of(new BusinessException.Violation("ROUTINE_DELETED", "Routine is deleted")));
        }

        if (routine.getStatus() != RoutineStatus.ACTIVE) {
            routine.setStatus(RoutineStatus.ACTIVE);
            routine.setPausedAt(null);
            routine = routineRepository.save(routine);
            log.info("Routine resumed: id={}, householdId={}", routineId, householdId);
        }

        return routine;
    }

    @Transactional(readOnly = true)
    public UpcomingInstancesResponse getUpcomingInstances(UUID routineId, UUID householdId, int days) {
        Routine routine = getRoutine(routineId, householdId);

        RecurrenceRule rule;
        try {
            rule = objectMapper.readValue(routine.getRecurrenceRuleJson(), RecurrenceRule.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid recurrence rule JSON", e);
        }

        int count = Math.min(Math.max(days, 1), 30);
        List<LocalDate> dates = recurrenceRuleParser.getOccurrencesInRange(rule, LocalDate.now(), count);

        UserSummaryDto assignee = routine.getAssignmentPolicy() == AssignmentPolicy.FIXED
                ? UserSummaryDto.from(routine.getFixedAssignee())
                : null;

        List<UpcomingInstanceDto> instances = dates.stream()
                .map(date -> new UpcomingInstanceDto(date, assignee))
                .toList();

        // TODO(ST-1008): add alreadyGenerated flag when task lookup exists.
        return new UpcomingInstancesResponse(routine.getId(), routine.getTitle(), instances);
    }

    private Zone resolveZone(UUID zoneId, UUID householdId) {
        return zoneService
                .findByIdAndHouseholdId(zoneId, householdId)
                .orElseThrow(() -> businessViolation("ZONE_MUST_EXIST", "Zone does not exist in household"));
    }

    private void applyAssignmentPolicy(
            Routine routine,
            AssignmentPolicy policy,
            UUID fixedAssigneeId,
            UUID householdId,
            boolean creating,
            boolean policyChanged) {
        if (policy == AssignmentPolicy.FIXED) {
            if (fixedAssigneeId == null) {
                if (!creating && routine.getFixedAssignee() != null) {
                    return;
                }
                throw businessViolation("ASSIGNEE_MUST_BE_MEMBER", "Fixed assignee is required for FIXED policy");
            }

            if (!membershipService.isMember(fixedAssigneeId, householdId)) {
                throw businessViolation("ASSIGNEE_MUST_BE_MEMBER", "Fixed assignee must be household member");
            }

            User assignee = userService.getById(fixedAssigneeId);
            routine.setFixedAssignee(assignee);
        } else if (creating || policyChanged) {
            routine.setFixedAssignee(null);
        }
    }

    private void validateRecurrenceRule(RecurrenceRule rule) {
        if (rule instanceof RecurrenceRule.Weekly weekly) {
            List<DayOfWeek> daysOfWeek = weekly.daysOfWeek();
            if (daysOfWeek == null || daysOfWeek.isEmpty()) {
                throw businessViolation(
                        ErrorCode.INVALID_RECURRENCE_RULE.name(), "daysOfWeek is required for WEEKLY type");
            }
        } else if (rule instanceof RecurrenceRule.Monthly monthly) {
            int day = monthly.dayOfMonth();
            if (day < 1 || day > 31) {
                throw businessViolation(
                        ErrorCode.INVALID_RECURRENCE_RULE.name(), "dayOfMonth must be between 1 and 31");
            }
        } else if (rule instanceof RecurrenceRule.EveryNDays everyNDays) {
            int interval = everyNDays.interval();
            if (interval < 2 || interval > 365) {
                throw businessViolation(ErrorCode.INVALID_RECURRENCE_RULE.name(), "interval must be between 2 and 365");
            }
        }
    }

    private String serializeRecurrenceRule(RecurrenceRule rule) {
        try {
            return objectMapper.writeValueAsString(rule);
        } catch (JsonProcessingException e) {
            throw businessViolation(ErrorCode.INVALID_RECURRENCE_RULE.name(), "Invalid recurrence rule");
        }
    }

    private BusinessException businessViolation(String rule, String message) {
        return new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION, message, List.of(new BusinessException.Violation(rule, message)));
    }
}
