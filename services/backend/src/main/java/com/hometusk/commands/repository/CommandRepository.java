package com.hometusk.commands.repository;

import com.hometusk.commands.domain.Command;
import com.hometusk.commands.domain.CommandStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandRepository extends JpaRepository<Command, UUID> {

    Optional<Command> findByCorrelationId(UUID correlationId);

    boolean existsByCorrelationId(UUID correlationId);

    List<Command> findByHouseholdIdOrderByCreatedAtDesc(UUID householdId);

    List<Command> findByHouseholdIdAndStatusOrderByCreatedAtDesc(UUID householdId, CommandStatus status);

    List<Command> findByRequesterIdOrderByCreatedAtDesc(UUID requesterId);
}
