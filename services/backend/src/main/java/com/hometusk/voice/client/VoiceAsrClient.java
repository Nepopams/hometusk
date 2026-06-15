package com.hometusk.voice.client;

import com.hometusk.voice.dto.VoiceAsrTranscription;

public interface VoiceAsrClient {

    VoiceAsrTranscription transcribe(byte[] fileBytes, String fileName, String contentType, String correlationId);
}
