package com.hometusk.commands.repository;

import com.hometusk.commands.domain.CommandConfirmation;
import com.hometusk.commands.domain.CommandConfirmationStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

@Repository
public interface CommandConfirmationRepository extends JpaRepository<CommandConfirmation, UUID> {

    Optional<CommandConfirmation> findByIdAndCommand_Id(UUID id, UUID commandId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CommandConfirmation> findWithLockByIdAndCommand_Id(UUID id, UUID commandId);

    List<CommandConfirmation> findByCommand_IdOrderByCreatedAtDesc(UUID commandId);

    List<CommandConfirmation> findByHouseholdIdAndStatusOrderByCreatedAtDesc(
            UUID householdId, CommandConfirmationStatus status);
}
