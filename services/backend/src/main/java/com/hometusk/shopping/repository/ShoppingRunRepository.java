package com.hometusk.shopping.repository;

import com.hometusk.shopping.domain.ShoppingRun;
import com.hometusk.shopping.domain.ShoppingRunStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShoppingRunRepository extends JpaRepository<ShoppingRun, UUID> {

    Optional<ShoppingRun> findByIdAndHousehold_Id(UUID id, UUID householdId);

    List<ShoppingRun> findByHousehold_IdOrderByCreatedAtDesc(UUID householdId);

    List<ShoppingRun> findByHousehold_IdAndStatusOrderByCreatedAtDesc(UUID householdId, ShoppingRunStatus status);

    boolean existsByIdAndHousehold_Id(UUID id, UUID householdId);
}
