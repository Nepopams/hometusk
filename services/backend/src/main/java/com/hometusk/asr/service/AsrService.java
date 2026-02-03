package com.hometusk.asr.service;

import com.hometusk.asr.client.AsrClient;
import com.hometusk.asr.domain.AsrIdempotencyRecord;
import com.hometusk.asr.domain.AsrTranscriptionRef;
import com.hometusk.asr.dto.AsrJobCreated;
import com.hometusk.asr.dto.AsrJobResult;
import com.hometusk.asr.dto.CreateTranscriptionResponse;
import com.hometusk.asr.dto.TranscriptionResultResponse;
import com.hometusk.asr.exception.AsrNotFoundException;
import com.hometusk.asr.metrics.AsrMetrics;
import com.hometusk.asr.repository.AsrTranscriptionRefRepository;
import com.hometusk.shared.logging.MdcKeys;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsrService {

    private static final String ASR_NOT_FOUND_CODE = "ASR_NOT_FOUND";
    private static final Logger log = LoggerFactory.getLogger(AsrService.class);

    private final AsrClient asrClient;
    private final AsrTranscriptionRefRepository refRepository;
    private final AsrIdempotencyService idempotencyService;
    private final AsrMetrics metrics;

    public AsrService(
            AsrClient asrClient,
            AsrTranscriptionRefRepository refRepository,
            AsrIdempotencyService idempotencyService,
            AsrMetrics metrics) {
        this.asrClient = asrClient;
        this.refRepository = refRepository;
        this.idempotencyService = idempotencyService;
        this.metrics = metrics;
    }

    @Transactional
    public CreateTranscriptionResponse createTranscription(
            UUID householdId,
            UUID userId,
            byte[] fileBytes,
            String fileName,
            String contentType,
            String languageHint,
            String correlationId,
            String idempotencyKey) {
        long startTime = System.currentTimeMillis();
        long sizeBytes = fileBytes != null ? fileBytes.length : 0L;

        try {
            Optional<AsrIdempotencyRecord> existing =
                    idempotencyService.findReusableRecord(householdId, userId, idempotencyKey, fileBytes);
            if (existing.isPresent()) {
                AsrIdempotencyRecord record = existing.get();
                Instant createdAt = record.getCreatedAt() != null ? record.getCreatedAt() : Instant.now();
                CreateTranscriptionResponse response =
                        new CreateTranscriptionResponse(record.getTranscriptionId(), "queued", createdAt);

                metrics.recordRequest("create", true);
                log.info(
                        "ASR create cached: status=success, jobStatus=queued, transcriptionId={}, sizeBytes={}, durationMs={}, correlationId={}, userId={}, householdId={}",
                        record.getTranscriptionId(),
                        sizeBytes,
                        System.currentTimeMillis() - startTime,
                        correlationId,
                        userId,
                        householdId);
                return response;
            }

            AsrJobCreated job = asrClient.createTranscription(
                    fileBytes, fileName, contentType, languageHint, correlationId, idempotencyKey);

            AsrTranscriptionRef ref = new AsrTranscriptionRef(job.id(), householdId, userId);
            refRepository.save(ref);
            idempotencyService.storeRecord(householdId, userId, idempotencyKey, fileBytes, job.id(), job.createdAt());

            metrics.recordRequest("create", true);
            log.info(
                    "ASR create completed: status=success, jobStatus={}, transcriptionId={}, sizeBytes={}, durationMs={}, correlationId={}, userId={}, householdId={}",
                    job.status(),
                    job.id(),
                    sizeBytes,
                    System.currentTimeMillis() - startTime,
                    correlationId,
                    userId,
                    householdId);
            return CreateTranscriptionResponse.from(job);
        } catch (RuntimeException ex) {
            String reason = AsrMetrics.reasonFromException(ex);
            metrics.recordRequest("create", false);
            metrics.recordFailure(reason);
            log.error(
                    "ASR create failed: status=error, reason={}, sizeBytes={}, durationMs={}, correlationId={}, userId={}, householdId={}",
                    reason,
                    sizeBytes,
                    System.currentTimeMillis() - startTime,
                    correlationId,
                    userId,
                    householdId);
            throw ex;
        }
    }

    public TranscriptionResultResponse getTranscription(UUID householdId, UUID transcriptionId, String correlationId) {
        long startTime = System.currentTimeMillis();
        String userId = MDC.get(MdcKeys.USER_ID);

        try {
            AsrTranscriptionRef ref = refRepository
                    .findByTranscriptionId(transcriptionId)
                    .orElseThrow(() -> new AsrNotFoundException(ASR_NOT_FOUND_CODE, "Transcription not found"));

            if (!ref.getHouseholdId().equals(householdId)) {
                throw new AsrNotFoundException(ASR_NOT_FOUND_CODE, "Transcription not found");
            }

            AsrJobResult job = asrClient.getTranscription(transcriptionId.toString(), correlationId);
            metrics.recordRequest("poll", true);
            log.info(
                    "ASR poll completed: status=success, jobStatus={}, transcriptionId={}, sizeBytes=0, durationMs={}, correlationId={}, userId={}, householdId={}",
                    job.status(),
                    transcriptionId,
                    System.currentTimeMillis() - startTime,
                    correlationId,
                    userId,
                    householdId);
            return TranscriptionResultResponse.from(job);
        } catch (RuntimeException ex) {
            String reason = AsrMetrics.reasonFromException(ex);
            metrics.recordRequest("poll", false);
            metrics.recordFailure(reason);
            log.error(
                    "ASR poll failed: status=error, reason={}, transcriptionId={}, sizeBytes=0, durationMs={}, correlationId={}, userId={}, householdId={}",
                    reason,
                    transcriptionId,
                    System.currentTimeMillis() - startTime,
                    correlationId,
                    userId,
                    householdId);
            throw ex;
        }
    }
}
