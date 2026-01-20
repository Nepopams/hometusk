package com.hometusk.households.service;

import com.hometusk.households.domain.Household;
import com.hometusk.households.repository.HouseholdRepository;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HouseholdService {

    private final HouseholdRepository householdRepository;

    public HouseholdService(HouseholdRepository householdRepository) {
        this.householdRepository = householdRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Household> findById(UUID id) {
        return householdRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Household getById(UUID id) {
        return householdRepository
                .findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorCode.HOUSEHOLD_NOT_FOUND, "Household not found: " + id));
    }

    @Transactional(readOnly = true)
    public boolean exists(UUID id) {
        return householdRepository.existsById(id);
    }

    @Transactional
    public Household create(String name) {
        Household household = new Household(name);
        return householdRepository.save(household);
    }

    @Transactional
    public Household update(Household household) {
        return householdRepository.save(household);
    }
}
