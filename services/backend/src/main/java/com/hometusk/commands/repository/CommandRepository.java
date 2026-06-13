package com.hometusk.commands.repository;

import com.hometusk.commands.domain.Command;
import com.hometusk.commands.domain.CommandStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandRepository extends JpaRepository<Command, UUID> {

    Optional<Command> findByCorrelationId(UUID correlationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Command c where c.id = :id")
    Optional<Command> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByCorrelationId(UUID correlationId);

    List<Command> findByHousehold_IdOrderByCreatedAtDesc(UUID householdId);

    List<Command> findByHousehold_IdAndStatusOrderByCreatedAtDesc(UUID householdId, CommandStatus status);

    List<Command> findByRequester_IdOrderByCreatedAtDesc(UUID requesterId);

    @Query("select c.id from Command c where c.status = :status and c.scheduleAt <= :now order by c.scheduleAt asc")
    List<UUID> findDueScheduledCommandIds(
            @Param("status") CommandStatus status, @Param("now") Instant now, Pageable pageable);
}
