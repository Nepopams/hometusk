package com.hometusk.voice.service;

import com.hometusk.voice.client.VoiceAsrClient;
import com.hometusk.voice.config.VoiceProperties;
import com.hometusk.voice.dto.VoiceAsrTranscription;
import com.hometusk.voice.dto.VoiceTranscriptionResponse;
import com.hometusk.voice.exception.VoiceAsrException;
import com.hometusk.voice.metrics.VoiceMetrics;
import java.io.IOException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VoiceTranscriptionService {

    private final VoiceProperties properties;
    private final VoiceAudioValidationService validationService;
    private final VoiceRateLimitService rateLimitService;
    private final VoiceAsrClient asrClient;
    private final VoiceMetrics metrics;

    public VoiceTranscriptionService(
            VoiceProperties properties,
            VoiceAudioValidationService validationService,
            VoiceRateLimitService rateLimitService,
            VoiceAsrClient asrClient,
            VoiceMetrics metrics) {
        this.properties = properties;
        this.validationService = validationService;
        this.rateLimitService = rateLimitService;
        this.asrClient = asrClient;
        this.metrics = metrics;
    }

    public VoiceTranscriptionResponse transcribe(MultipartFile file, UUID userId, String correlationId)
            throws IOException {
        long startTime = System.currentTimeMillis();
        try {
            if (!properties.isEnabled() || !properties.asr().isEnabled()) {
                throw VoiceAsrException.configError("Voice ASR is disabled");
            }

            rateLimitService.checkLimit(userId);
            validationService.validate(file);
            metrics.recordFileSize(file.getSize());

            VoiceAsrTranscription transcription = asrClient.transcribe(
                    file.getBytes(), file.getOriginalFilename(), file.getContentType(), correlationId);

            long latencyMs = Math.max(0L, System.currentTimeMillis() - startTime);
            metrics.recordAsrRequest("ok");
            metrics.recordAsrLatency(latencyMs);

            return new VoiceTranscriptionResponse(transcription.transcript(), "ok", transcription.traceId(), latencyMs);
        } catch (VoiceAsrException ex) {
            long latencyMs = Math.max(0L, System.currentTimeMillis() - startTime);
            metrics.recordAsrRequest("error");
            metrics.recordAsrError(ex.getCode());
            metrics.recordAsrLatency(latencyMs);
            throw ex;
        }
    }
}
