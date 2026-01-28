package com.hometusk.gamification.service;

import com.hometusk.gamification.domain.GamificationSettings;
import com.hometusk.gamification.dto.GamificationSettingsDto;
import com.hometusk.gamification.repository.GamificationSettingsRepository;
import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GamificationSettingsService {

    private static final Logger log = LoggerFactory.getLogger(GamificationSettingsService.class);

    private final GamificationSettingsRepository repository;

    public GamificationSettingsService(GamificationSettingsRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public GamificationSettings getOrCreate(User user, Household household) {
        return repository
                .findByUser_IdAndHousehold_Id(user.getId(), household.getId())
                .orElseGet(() -> {
                    try {
                        GamificationSettings settings = new GamificationSettings(user, household);
                        GamificationSettings saved = repository.save(settings);
                        log.info(
                                "Created gamification settings for user {} in household {}",
                                user.getId(),
                                household.getId());
                        return saved;
                    } catch (DataIntegrityViolationException e) {
                        log.debug("Settings already created by concurrent request, re-fetching");
                        return repository
                                .findByUser_IdAndHousehold_Id(user.getId(), household.getId())
                                .orElseThrow(() -> new IllegalStateException("Settings should exist"));
                    }
                });
    }

    @Transactional
    public GamificationSettings update(User user, Household household, GamificationSettingsDto request) {
        GamificationSettings settings = getOrCreate(user, household);
        settings.setShowProgressToOthers(request.showProgressToOthers());
        settings.setGamificationEnabled(request.gamificationEnabled());
        settings.setStreakVisible(request.streakVisible());
        return repository.save(settings);
    }

    public boolean isGamificationEnabled(User user, Household household) {
        return repository
                .findByUser_IdAndHousehold_Id(user.getId(), household.getId())
                .map(GamificationSettings::isGamificationEnabled)
                .orElse(true);
    }

    public boolean isStreakVisible(User user, Household household) {
        return repository
                .findByUser_IdAndHousehold_Id(user.getId(), household.getId())
                .map(GamificationSettings::isStreakVisible)
                .orElse(true);
    }
}
