package com.hometusk.gamification.service;

import com.hometusk.gamification.domain.Badge;
import com.hometusk.gamification.domain.PointsReason;
import com.hometusk.gamification.domain.UserBadge;
import com.hometusk.gamification.dto.BadgeCatalogResponse;
import com.hometusk.gamification.dto.BadgeDto;
import com.hometusk.gamification.repository.BadgeRepository;
import com.hometusk.gamification.repository.PointsLedgerRepository;
import com.hometusk.gamification.repository.UserBadgeRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.notifications.service.NotificationService;
import com.hometusk.users.domain.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BadgeService {

    private static final String FIRST_TASK = "FIRST_TASK";
    private static final String TEN_TASKS = "TEN_TASKS";
    private static final String WEEK_WARRIOR = "WEEK_WARRIOR";
    private static final String ZONE_SPECIALIST = "ZONE_SPECIALIST";
    private static final String ON_TIME_HERO = "ON_TIME_HERO";

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final NotificationService notificationService;

    public BadgeService(
            BadgeRepository badgeRepository,
            UserBadgeRepository userBadgeRepository,
            PointsLedgerRepository pointsLedgerRepository,
            NotificationService notificationService) {
        this.badgeRepository = badgeRepository;
        this.userBadgeRepository = userBadgeRepository;
        this.pointsLedgerRepository = pointsLedgerRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void checkAndAwardBadges(User user, Household household) {
        if (user == null || household == null) {
            return;
        }

        UUID userId = user.getId();
        UUID householdId = household.getId();
        Set<String> earned = new HashSet<>(userBadgeRepository.findBadgeCodesByUserAndHousehold(userId, householdId));

        long completedCount = pointsLedgerRepository.countByUser_IdAndHousehold_IdAndReason(
                userId, householdId, PointsReason.TASK_COMPLETED);
        long onTimeCount = pointsLedgerRepository.countByUser_IdAndHousehold_IdAndReason(
                userId, householdId, PointsReason.ON_TIME_BONUS);

        checkAndAward(FIRST_TASK, earned, user, household, completedCount >= 1);
        checkAndAward(TEN_TASKS, earned, user, household, completedCount >= 10);

        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        checkAndAward(
                WEEK_WARRIOR,
                earned,
                user,
                household,
                pointsLedgerRepository.countByUser_IdAndHousehold_IdAndReasonAndCreatedAtAfter(
                                userId, householdId, PointsReason.TASK_COMPLETED, since)
                        >= 7);

        checkAndAward(ZONE_SPECIALIST, earned, user, household, maxZoneCount(userId, householdId) >= 5);

        checkAndAward(ON_TIME_HERO, earned, user, household, onTimeCount >= 5);
    }

    @Transactional(readOnly = true)
    public List<BadgeDto> getEarnedBadges(UUID userId, UUID householdId) {
        List<UserBadge> earned = userBadgeRepository.findByUserAndHouseholdWithBadge(userId, householdId);
        return earned.stream()
                .map(userBadge -> BadgeDto.from(userBadge.getBadge(), userBadge.getEarnedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public BadgeCatalogResponse getBadgeCatalog(UUID userId, UUID householdId) {
        List<Badge> badges = badgeRepository.findAllByOrderByNameAsc();
        Map<String, Instant> earned = loadEarnedMap(userId, householdId);

        List<BadgeDto> items = badges.stream()
                .map(badge -> BadgeDto.from(badge, earned.get(badge.getCode())))
                .toList();

        return new BadgeCatalogResponse(items);
    }

    private void checkAndAward(
            String badgeCode, Set<String> earned, User user, Household household, boolean criteriaMet) {
        if (earned.contains(badgeCode) || !criteriaMet) {
            return;
        }

        Badge badge = badgeRepository.findByCode(badgeCode).orElse(null);
        if (badge == null) {
            return;
        }

        try {
            userBadgeRepository.save(new UserBadge(user, household, badge));
            earned.add(badgeCode);
            notificationService.notifyBadgeEarned(user, household, badge);
        } catch (DataIntegrityViolationException e) {
            // Badge already awarded (race)
        }
    }

    private int maxZoneCount(UUID userId, UUID householdId) {
        List<Object[]> rows =
                pointsLedgerRepository.countCompletedByZone(userId, householdId, PointsReason.TASK_COMPLETED);
        int max = 0;
        for (Object[] row : rows) {
            if (row.length < 2) {
                continue;
            }
            Long count = (Long) row[1];
            if (count != null && count > max) {
                max = count.intValue();
            }
        }
        return max;
    }

    private Map<String, Instant> loadEarnedMap(UUID userId, UUID householdId) {
        List<UserBadge> earned = userBadgeRepository.findByUserAndHouseholdWithBadge(userId, householdId);
        Map<String, Instant> map = new HashMap<>();
        for (UserBadge badge : earned) {
            map.put(badge.getBadge().getCode(), badge.getEarnedAt());
        }
        return map;
    }
}
