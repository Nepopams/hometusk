package com.hometusk.households.repository;

import com.hometusk.households.domain.Zone;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, UUID> {

    List<Zone> findByHouseholdId(UUID householdId);

    Optional<Zone> findByIdAndHouseholdId(UUID id, UUID householdId);

    boolean existsByIdAndHouseholdId(UUID id, UUID householdId);

    Optional<Zone> findByHouseholdIdAndName(UUID householdId, String name);
}
