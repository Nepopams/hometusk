package com.hometusk.commands.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hometusk.commands.dto.CommandRequest;
import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.ValidationException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommandIdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(CommandIdempotencyService.class);
    private static final Duration TTL = Duration.ofHours(24);
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,128}$");

    private final CommandIdempotencyRepository repository;
    private final ObjectMapper objectMapper;

    public CommandIdempotencyService(CommandIdempotencyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Optional<IdempotencySession> begin(
            List<String> idempotencyKeys, UUID initiatorUserId, CommandRequest request) {
        if (idempotencyKeys == null || idempotencyKeys.isEmpty()) {
            return Optional.empty();
        }
        if (idempotencyKeys.size() > 1) {
            throw new ValidationException(
                    "$.headers.Idempotency-Key", "MULTIPLE_HEADERS", "Idempotency-Key must be provided once");
        }

        String idempotencyKey = idempotencyKeys.get(0);
        validateKey(idempotencyKey);

        String requestHash = hashRequest(request);
        return Optional.of(createOrReplay(idempotencyKey, initiatorUserId, requestHash));
    }

    @Transactional
    public void storeResponse(UUID idempotencyId, int httpStatus, Object responseBody) {
        repository.findById(idempotencyId).ifPresent(record -> {
            try {
                String responseJson = objectMapper.writeValueAsString(responseBody);
                record.storeResponse(responseJson, httpStatus);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize idempotency response: id={}", idempotencyId, e);
            }
        });
    }

    private IdempotencySession createOrReplay(String idempotencyKey, UUID initiatorUserId, String requestHash) {
        Instant now = Instant.now();

        for (int attempt = 0; attempt < 2; attempt++) {
            UUID recordId = UUID.randomUUID();
            int inserted = repository.insertIfNotExists(
                    recordId, idempotencyKey, initiatorUserId, requestHash, now, now.plus(TTL));
            if (inserted > 0) {
                return IdempotencySession.newRequest(recordId);
            }

            Optional<CommandIdempotency> existingOpt =
                    repository.findByIdempotencyKeyAndInitiatorUserId(idempotencyKey, initiatorUserId);
            if (existingOpt.isEmpty()) {
                log.warn("Idempotency conflict but no record found: key={}", idempotencyKey);
                throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
            }

            CommandIdempotency existing = existingOpt.get();
            if (existing.isExpired(now)) {
                repository.delete(existing);
                repository.flush();
                continue;
            }

            if (!existing.getRequestHash().equals(requestHash)) {
                throw new BusinessException(
                        ErrorCode.IDEMPOTENCY_CONFLICT, "Idempotency-Key reused with different payload");
            }

            if (existing.hasStoredResponse()) {
                return IdempotencySession.replay(
                        existing.getId(),
                        new StoredResponse(existing.getStoredHttpStatus(), existing.getStoredResponseJson()));
            }

            throw new BusinessException(
                    ErrorCode.IDEMPOTENCY_CONFLICT, "Idempotency-Key request already in progress");
        }

        throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT, "Unable to create idempotency record");
    }

    private void validateKey(String idempotencyKey) {
        if (idempotencyKey == null || !KEY_PATTERN.matcher(idempotencyKey).matches()) {
            throw new ValidationException(
                    "$.headers.Idempotency-Key",
                    "INVALID_HEADER",
                    "Idempotency-Key must match ^[A-Za-z0-9._-]{1,128}$");
        }
    }

    private String hashRequest(CommandRequest request) {
        try {
            ObjectMapper hashingMapper = objectMapper.copy();
            hashingMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
            hashingMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            byte[] json = hashingMapper.writeValueAsBytes(request);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash request for idempotency", e);
        }
    }

    public record IdempotencySession(UUID recordId, StoredResponse storedResponse) {
        public static IdempotencySession newRequest(UUID recordId) {
            return new IdempotencySession(recordId, null);
        }

        public static IdempotencySession replay(UUID recordId, StoredResponse storedResponse) {
            return new IdempotencySession(recordId, storedResponse);
        }

        public boolean isReplay() {
            return storedResponse != null;
        }
    }

    public record StoredResponse(int httpStatus, String bodyJson) {}
}
