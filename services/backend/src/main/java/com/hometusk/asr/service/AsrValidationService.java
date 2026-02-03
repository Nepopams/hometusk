package com.hometusk.asr.service;

import com.hometusk.asr.client.AsrProperties;
import com.hometusk.asr.exception.AsrFileTooLargeException;
import com.hometusk.asr.exception.AsrInvalidFormatException;
import com.hometusk.asr.exception.AsrMissingFileException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AsrValidationService {

    private final long maxSizeBytes;
    private final Set<String> allowedFormats;

    public AsrValidationService(AsrProperties properties) {
        this.maxSizeBytes = properties.guardrails().maxSizeBytes();
        this.allowedFormats = properties.guardrails().allowedFormats().stream()
                .filter(Objects::nonNull)
                .map(format -> format.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AsrMissingFileException();
        }

        if (file.getSize() > maxSizeBytes) {
            long maxMb = maxSizeBytes / (1024 * 1024);
            String message = "File size exceeds maximum of " + maxMb + " MB";
            throw new AsrFileTooLargeException("ASR_FILE_TOO_LARGE", message);
        }

        String contentType = normalizeContentType(file.getContentType());
        if (contentType == null || !allowedFormats.contains(contentType)) {
            String message = "Unsupported audio format. Supported: " + String.join(", ", allowedFormats);
            throw new AsrInvalidFormatException("ASR_INVALID_FORMAT", message);
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
