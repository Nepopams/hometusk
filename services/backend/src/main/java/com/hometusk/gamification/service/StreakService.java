package com.hometusk.gamification.service;

import com.hometusk.gamification.domain.StreakState;
import com.hometusk.gamification.repository.StreakStateRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StreakService {

    private static final Logger log = LoggerFactory.getLogger(StreakService.class);

    private final StreakStateRepository repository;
    private final GamificationSettingsService settingsService;

    public StreakService(StreakStateRepository repository, GamificationSettingsService settingsService) {
        this.repository = repository;
        this.settingsService = settingsService;
    }

    @Transactional
    public StreakState getOrCreate(User user, Household household) {
        return repository
                .findByUser_IdAndHousehold_Id(user.getId(), household.getId())
                .orElseGet(() -> {
                    try {
                        StreakState state = new StreakState(user, household);
                        StreakState saved = repository.save(state);
                        log.info("Created streak state for user {} in household {}", user.getId(), household.getId());
                        return saved;
                    } catch (DataIntegrityViolationException e) {
                        log.debug("Streak state already created by concurrent request, re-fetching");
                        return repository
                                .findByUser_IdAndHousehold_Id(user.getId(), household.getId())
                                .orElseThrow(() -> new IllegalStateException("Streak state should exist"));
                    }
                });
    }

    @Transactional
    public void updateStreak(User user, Household household, LocalDate activityDate) {
        if (user == null || household == null || activityDate == null) {
            return;
        }

        if (!settingsService.isGamificationEnabled(user, household)) {
            log.debug("Gamification disabled for user {}, skipping streak update", user.getId());
            return;
        }

        StreakState state = getOrCreate(user, household);
        LocalDate lastActivity = state.getLastActivityDate();

        if (lastActivity == null) {
            state.setCurrentStreak(1);
            state.setGraceUsedToday(false);
            state.setLastActivityDate(activityDate);
            if (state.getCurrentStreak() > state.getBestStreak()) {
                state.setBestStreak(state.getCurrentStreak());
            }
            repository.save(state);
            return;
        }

        long daysSince = ChronoUnit.DAYS.between(lastActivity, activityDate);

        if (daysSince <= 0) {
            return;
        }

        if (daysSince == 1) {
            state.setCurrentStreak(state.getCurrentStreak() + 1);
            state.setGraceUsedToday(false);
        } else if (daysSince == 2 && !state.isGraceUsedToday()) {
            state.setCurrentStreak(state.getCurrentStreak() + 1);
            state.setGraceUsedToday(true);
        } else {
            if (state.getCurrentStreak() > state.getBestStreak()) {
                state.setBestStreak(state.getCurrentStreak());
            }
            state.setCurrentStreak(1);
            state.setGraceUsedToday(false);
        }

        if (state.getCurrentStreak() > state.getBestStreak()) {
            state.setBestStreak(state.getCurrentStreak());
        }

        state.setLastActivityDate(activityDate);
        repository.save(state);
    }

    public StreakState getStreakState(User user, Household household) {
        return getOrCreate(user, household);
    }
}
