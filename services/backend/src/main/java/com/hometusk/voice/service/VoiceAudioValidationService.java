package com.hometusk.voice.service;

import com.hometusk.voice.config.VoiceProperties;
import com.hometusk.voice.exception.VoiceAsrException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VoiceAudioValidationService {

    private final long maxSizeBytes;
    private final Set<String> allowedMediaTypes;

    public VoiceAudioValidationService(VoiceProperties properties) {
        this.maxSizeBytes = properties.asr().maxSizeBytes();
        this.allowedMediaTypes = properties.asr().allowedMediaTypes().stream()
                .filter(Objects::nonNull)
                .map(type -> type.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw VoiceAsrException.missingAudioFile();
        }

        if (file.getSize() > maxSizeBytes) {
            long maxMb = maxSizeBytes / (1024 * 1024);
            throw VoiceAsrException.fileTooLarge("File size exceeds maximum of " + maxMb + " MB");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (contentType == null || !allowedMediaTypes.contains(contentType)) {
            throw VoiceAsrException.unsupportedMedia("Unsupported audio media type");
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String normalized = contentType.split(";")[0].trim();
        return normalized.isEmpty() ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
