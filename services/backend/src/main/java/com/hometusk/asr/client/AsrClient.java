package com.hometusk.asr.client;

import com.hometusk.asr.dto.AsrJobCreated;
import com.hometusk.asr.dto.AsrJobResult;

public interface AsrClient {

    AsrJobCreated createTranscription(
            byte[] fileBytes,
            String fileName,
            String contentType,
            String languageHint,
            String correlationId,
            String idempotencyKey);

    AsrJobResult getTranscription(String transcriptionId, String correlationId);
}
