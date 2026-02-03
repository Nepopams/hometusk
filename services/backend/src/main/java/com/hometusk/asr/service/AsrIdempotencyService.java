package com.hometusk.asr.service;

import com.hometusk.asr.domain.AsrIdempotencyRecord;
import com.hometusk.asr.exception.AsrIdempotencyConflictException;
import com.hometusk.asr.repository.AsrIdempotencyRecordRepository;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsrIdempotencyService {

    private static final Duration TTL = Duration.ofHours(24);

    private final AsrIdempotencyRecordRepository repository;

    public AsrIdempotencyService(AsrIdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Optional<AsrIdempotencyRecord> findReusableRecord(
            UUID householdId, UUID userId, String idempotencyKey, byte[] fileBytes) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        String digest = computeDigest(fileBytes);
        Optional<AsrIdempotencyRecord> existing =
                repository.findByHouseholdIdAndUserIdAndIdempotencyKey(householdId, userId, idempotencyKey);

        if (existing.isEmpty()) {
            return Optional.empty();
        }

        AsrIdempotencyRecord record = existing.get();
        Instant now = Instant.now();
        if (record.isExpired(now)) {
            repository.delete(record);
            return Optional.empty();
        }

        if (!record.getPayloadDigest().equals(digest)) {
            throw new AsrIdempotencyConflictException();
        }

        return Optional.of(record);
    }

    @Transactional
    public void storeRecord(
            UUID householdId,
            UUID userId,
            String idempotencyKey,
            byte[] fileBytes,
            UUID transcriptionId,
            Instant createdAt) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }

        String digest = computeDigest(fileBytes);
        Optional<AsrIdempotencyRecord> existing =
                repository.findByHouseholdIdAndUserIdAndIdempotencyKey(householdId, userId, idempotencyKey);
        Instant now = Instant.now();

        if (existing.isPresent()) {
            AsrIdempotencyRecord record = existing.get();
            if (record.isExpired(now)) {
                repository.delete(record);
            } else if (!record.getPayloadDigest().equals(digest)) {
                throw new AsrIdempotencyConflictException();
            } else {
                return;
            }
        }

        Instant effectiveCreatedAt = createdAt != null ? createdAt : now;
        AsrIdempotencyRecord record = new AsrIdempotencyRecord(
                householdId,
                userId,
                idempotencyKey,
                digest,
                transcriptionId,
                effectiveCreatedAt,
                effectiveCreatedAt.plus(TTL));

        try {
            repository.save(record);
        } catch (DataIntegrityViolationException e) {
            throw new AsrIdempotencyConflictException();
        }
    }

    private String computeDigest(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash file for idempotency", e);
        }
    }
}
