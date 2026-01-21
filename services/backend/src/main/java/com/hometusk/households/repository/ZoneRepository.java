package com.hometusk.households.repository;

import com.hometusk.households.domain.Zone;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, UUID> {

    List<Zone> findByHousehold_Id(UUID householdId);

    Optional<Zone> findByIdAndHousehold_Id(UUID id, UUID householdId);

    boolean existsByIdAndHousehold_Id(UUID id, UUID householdId);

    Optional<Zone> findByHousehold_IdAndName(UUID householdId, String name);
}
