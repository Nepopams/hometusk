package com.hometusk.gamification.service;

import com.hometusk.gamification.domain.PointsLedger;
import com.hometusk.gamification.domain.PointsReason;
import com.hometusk.gamification.dto.PointsEntryDto;
import com.hometusk.gamification.repository.PointsLedgerRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.tasks.domain.Task;
import com.hometusk.users.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointsService {

    private static final Logger log = LoggerFactory.getLogger(PointsService.class);
    private static final int BASE_POINTS = 10;
    private static final int ON_TIME_BONUS = 5;

    private final PointsLedgerRepository pointsLedgerRepository;
    private final GamificationSettingsService settingsService;

    public PointsService(PointsLedgerRepository pointsLedgerRepository, GamificationSettingsService settingsService) {
        this.pointsLedgerRepository = pointsLedgerRepository;
        this.settingsService = settingsService;
    }

    @Transactional
    public List<PointsLedger> awardForTaskCompleted(Task task, User actor) {
        if (task == null || task.getAssignee() == null) {
            return List.of();
        }

        List<PointsLedger> entries = new ArrayList<>();
        User assignee = task.getAssignee();
        Household household = task.getHousehold();

        if (!settingsService.isGamificationEnabled(assignee, household)) {
            log.debug("Gamification disabled for user {}, skipping points award", assignee.getId());
            return List.of();
        }

        PointsLedger base =
                awardPointsIdempotent(assignee, household, task, BASE_POINTS, PointsReason.TASK_COMPLETED, actor);
        if (base != null) {
            entries.add(base);
        }

        if (isOnTime(task)) {
            PointsLedger bonus =
                    awardPointsIdempotent(assignee, household, task, ON_TIME_BONUS, PointsReason.ON_TIME_BONUS, actor);
            if (bonus != null) {
                entries.add(bonus);
            }
        }

        return entries;
    }

    @Transactional
    public List<PointsLedger> reverseForTaskUncompleted(Task task, User actor) {
        if (task == null || task.getAssignee() == null) {
            return List.of();
        }

        List<PointsLedger> entries = new ArrayList<>();
        User assignee = task.getAssignee();
        Household household = task.getHousehold();

        if (pointsLedgerRepository
                .findByTask_IdAndUser_IdAndReason(task.getId(), assignee.getId(), PointsReason.TASK_COMPLETED)
                .isPresent()) {
            PointsLedger base = awardPointsIdempotent(
                    assignee, household, task, -BASE_POINTS, PointsReason.TASK_UNCOMPLETED, actor);
            if (base != null) {
                entries.add(base);
            }
        }

        if (pointsLedgerRepository
                .findByTask_IdAndUser_IdAndReason(task.getId(), assignee.getId(), PointsReason.ON_TIME_BONUS)
                .isPresent()) {
            PointsLedger bonus = awardPointsIdempotent(
                    assignee, household, task, -ON_TIME_BONUS, PointsReason.ON_TIME_BONUS_REVERSED, actor);
            if (bonus != null) {
                entries.add(bonus);
            }
        }

        return entries;
    }

    @Transactional(readOnly = true)
    public int getTotalPoints(UUID userId, UUID householdId) {
        return Math.toIntExact(pointsLedgerRepository.sumPointsByUserAndHousehold(userId, householdId));
    }

    @Transactional(readOnly = true)
    public int getPointsThisWeek(UUID userId, UUID householdId) {
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        return Math.toIntExact(pointsLedgerRepository.sumPointsByUserAndHouseholdSince(userId, householdId, since));
    }

    @Transactional(readOnly = true)
    public List<PointsEntryDto> getRecentActivity(UUID userId, UUID householdId, int limit) {
        List<PointsLedger> entries = pointsLedgerRepository.findByUser_IdAndHousehold_IdOrderByCreatedAtDesc(
                userId, householdId, PageRequest.of(0, limit));
        return entries.stream().map(PointsEntryDto::from).toList();
    }

    @Transactional(readOnly = true)
    public int getHouseholdTotalPoints(UUID householdId) {
        return Math.toIntExact(pointsLedgerRepository.sumPointsByHousehold(householdId));
    }

    @Transactional(readOnly = true)
    public int getHouseholdTotalTasks(UUID householdId) {
        return Math.toIntExact(
                pointsLedgerRepository.countByHousehold_IdAndReason(householdId, PointsReason.TASK_COMPLETED));
    }

    private boolean isOnTime(Task task) {
        return task.getDeadline() != null
                && task.getCompletedAt() != null
                && task.getCompletedAt().isBefore(task.getDeadline());
    }

    private PointsLedger awardPointsIdempotent(
            User user, Household household, Task task, int points, PointsReason reason, User actor) {
        Optional<PointsLedger> existing =
                pointsLedgerRepository.findByTask_IdAndUser_IdAndReason(task.getId(), user.getId(), reason);
        if (existing.isPresent()) {
            return existing.get();
        }

        PointsLedger entry = new PointsLedger(user, household, task, points, reason, actor, null);
        try {
            return pointsLedgerRepository.save(entry);
        } catch (DataIntegrityViolationException e) {
            return pointsLedgerRepository
                    .findByTask_IdAndUser_IdAndReason(task.getId(), user.getId(), reason)
                    .orElseThrow(() -> e);
        }
    }
}
