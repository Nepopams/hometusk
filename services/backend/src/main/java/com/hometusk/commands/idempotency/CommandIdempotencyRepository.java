package com.hometusk.commands.idempotency;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandIdempotencyRepository extends JpaRepository<CommandIdempotency, UUID> {

    Optional<CommandIdempotency> findByIdempotencyKeyAndInitiatorUserId(String idempotencyKey, UUID initiatorUserId);
}
