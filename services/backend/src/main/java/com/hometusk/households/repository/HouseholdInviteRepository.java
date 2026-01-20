package com.hometusk.households.repository;

import com.hometusk.households.domain.HouseholdInvite;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HouseholdInviteRepository extends JpaRepository<HouseholdInvite, UUID> {

    Optional<HouseholdInvite> findByInviteToken(String inviteToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from HouseholdInvite i where i.inviteToken = :inviteToken")
    Optional<HouseholdInvite> findByInviteTokenForUpdate(@Param("inviteToken") String inviteToken);
}
