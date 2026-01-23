package com.hometusk.notifications.repository;

import com.hometusk.notifications.domain.Notification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByHousehold_IdAndUser_Id(UUID householdId, UUID userId, Pageable pageable);

    List<Notification> findByHousehold_IdAndUser_IdAndCreatedAtAfter(
            UUID householdId, UUID userId, Instant createdAt, Pageable pageable);

    Optional<Notification> findByIdAndUser_Id(UUID id, UUID userId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Notification> findByIdempotencyKey(String idempotencyKey);
}
