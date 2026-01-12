package com.hometusk.users.repository;

import com.hometusk.users.domain.Membership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    List<Membership> findByUserId(UUID userId);

    List<Membership> findByHouseholdId(UUID householdId);

    Optional<Membership> findByUserIdAndHouseholdId(UUID userId, UUID householdId);

    boolean existsByUserIdAndHouseholdId(UUID userId, UUID householdId);

    @Query("SELECT m.household.id FROM Membership m WHERE m.user.id = :userId")
    List<UUID> findHouseholdIdsByUserId(@Param("userId") UUID userId);
}
