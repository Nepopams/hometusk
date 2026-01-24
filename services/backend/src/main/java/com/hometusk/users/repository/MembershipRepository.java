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

    List<Membership> findByUser_Id(UUID userId);

    List<Membership> findByHousehold_Id(UUID householdId);

    @Query("SELECT m FROM Membership m JOIN FETCH m.user WHERE m.household.id = :householdId")
    List<Membership> findByHousehold_IdWithUser(@Param("householdId") UUID householdId);

    Optional<Membership> findByUser_IdAndHousehold_Id(UUID userId, UUID householdId);

    boolean existsByUser_IdAndHousehold_Id(UUID userId, UUID householdId);

    @Query("SELECT m.household.id FROM Membership m WHERE m.user.id = :userId")
    List<UUID> findHouseholdIdsByUserId(@Param("userId") UUID userId);
}
