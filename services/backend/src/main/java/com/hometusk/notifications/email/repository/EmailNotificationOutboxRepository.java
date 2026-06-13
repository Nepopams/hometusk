package com.hometusk.notifications.email.repository;

import com.hometusk.notifications.email.domain.EmailNotificationOutbox;
import com.hometusk.notifications.email.domain.EmailNotificationStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
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
public interface EmailNotificationOutboxRepository extends JpaRepository<EmailNotificationOutbox, UUID> {

    Optional<EmailNotificationOutbox> findByIdempotencyKey(String idempotencyKey);

    long countByStatus(EmailNotificationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmailNotificationOutbox e where e.id = :id")
    Optional<EmailNotificationOutbox> findByIdForUpdate(@Param("id") UUID id);

    @Query(
            """
            select e.id from EmailNotificationOutbox e
            where e.status in :statuses
              and e.nextAttemptAt <= :now
            order by e.nextAttemptAt asc, e.createdAt asc
            """)
    List<UUID> findDueDeliveryIds(
            @Param("statuses") Collection<EmailNotificationStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable);
}
