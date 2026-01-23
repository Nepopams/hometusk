package com.hometusk.commands.idempotency;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommandIdempotencyRepository extends JpaRepository<CommandIdempotency, UUID> {

    Optional<CommandIdempotency> findByIdempotencyKeyAndInitiatorUserId(String idempotencyKey, UUID initiatorUserId);

    @Modifying
    @Query(
            value =
                    """
                INSERT INTO command_idempotency (
                    id, idempotency_key, initiator_user_id, request_hash, created_at, expires_at
                )
                VALUES (
                    :id, :idempotencyKey, :initiatorUserId, :requestHash, :createdAt, :expiresAt
                )
                ON CONFLICT (idempotency_key, initiator_user_id) DO NOTHING
                """,
            nativeQuery = true)
    int insertIfNotExists(
            @Param("id") UUID id,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("initiatorUserId") UUID initiatorUserId,
            @Param("requestHash") String requestHash,
            @Param("createdAt") Instant createdAt,
            @Param("expiresAt") Instant expiresAt);
}
