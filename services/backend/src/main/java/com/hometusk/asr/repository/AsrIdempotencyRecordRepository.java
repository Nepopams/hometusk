package com.hometusk.asr.repository;

import com.hometusk.asr.domain.AsrIdempotencyRecord;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AsrIdempotencyRecordRepository extends JpaRepository<AsrIdempotencyRecord, UUID> {

    Optional<AsrIdempotencyRecord> findByHouseholdIdAndUserIdAndIdempotencyKey(
            UUID householdId, UUID userId, String idempotencyKey);
}
