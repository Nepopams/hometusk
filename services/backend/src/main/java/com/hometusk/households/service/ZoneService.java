package com.hometusk.households.service;

import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.Zone;
import com.hometusk.households.repository.ZoneRepository;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ZoneService {

    private final ZoneRepository zoneRepository;

    public ZoneService(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    @Transactional(readOnly = true)
    public List<Zone> findByHouseholdId(UUID householdId) {
        return zoneRepository.findByHouseholdId(householdId);
    }

    @Transactional(readOnly = true)
    public Optional<Zone> findByIdAndHouseholdId(UUID id, UUID householdId) {
        return zoneRepository.findByIdAndHouseholdId(id, householdId);
    }

    @Transactional(readOnly = true)
    public Zone getByIdAndHouseholdId(UUID id, UUID householdId) {
        return zoneRepository
                .findByIdAndHouseholdId(id, householdId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.ZONE_NOT_FOUND, "Zone not found: " + id));
    }

    @Transactional(readOnly = true)
    public boolean existsInHousehold(UUID id, UUID householdId) {
        return zoneRepository.existsByIdAndHouseholdId(id, householdId);
    }

    @Transactional
    public Zone create(Household household, String name) {
        // Check for duplicate name
        Optional<Zone> existing = zoneRepository.findByHouseholdIdAndName(household.getId(), name);
        if (existing.isPresent()) {
            return existing.get();
        }

        Zone zone = new Zone(household, name);
        return zoneRepository.save(zone);
    }
}
