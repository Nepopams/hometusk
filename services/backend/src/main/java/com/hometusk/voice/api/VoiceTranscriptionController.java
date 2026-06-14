package com.hometusk.voice.api;

import com.hometusk.shared.logging.MdcKeys;
import com.hometusk.shared.security.CurrentUser;
import com.hometusk.users.service.UserResolver;
import com.hometusk.voice.dto.VoiceTranscriptionResponse;
import com.hometusk.voice.exception.VoiceAsrException;
import com.hometusk.voice.service.VoiceTranscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping("/api/v1/voice")
@Tag(name = "Voice", description = "Voice command transcription endpoints")
public class VoiceTranscriptionController {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final VoiceTranscriptionService transcriptionService;
    private final UserResolver userResolver;

    public VoiceTranscriptionController(VoiceTranscriptionService transcriptionService, UserResolver userResolver) {
        this.transcriptionService = transcriptionService;
        this.userResolver = userResolver;
    }

    @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create an editable voice command transcript draft")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transcript draft created"),
        @ApiResponse(responseCode = "400", description = "Invalid multipart request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "413", description = "Audio file too large"),
        @ApiResponse(responseCode = "415", description = "Unsupported audio media type"),
        @ApiResponse(responseCode = "429", description = "Local voice ASR rate limited"),
        @ApiResponse(responseCode = "502", description = "ASR provider error"),
        @ApiResponse(responseCode = "504", description = "ASR provider timeout")
    })
    public ResponseEntity<VoiceTranscriptionResponse> createTranscription(MultipartHttpServletRequest request)
            throws IOException {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        String correlationId = resolveCorrelationId();
        MultipartFile file = resolveSingleFile(request.getMultiFileMap());

        VoiceTranscriptionResponse response = transcriptionService.transcribe(file, currentUser.id(), correlationId);

        return ResponseEntity.ok().header(CORRELATION_ID_HEADER, correlationId).body(response);
    }

    private MultipartFile resolveSingleFile(MultiValueMap<String, MultipartFile> fileMap) {
        int totalFiles = fileMap.values().stream().mapToInt(List::size).sum();
        List<MultipartFile> files = fileMap.get("file");

        if (totalFiles == 0) {
            throw VoiceAsrException.missingAudioFile();
        }
        if (files == null || files.isEmpty()) {
            throw VoiceAsrException.invalidMultipart("Exactly one audio file field named file is required");
        }
        if (totalFiles != 1 || files.size() != 1) {
            throw VoiceAsrException.invalidMultipart("Exactly one audio file field named file is required");
        }
        return files.getFirst();
    }

    private String resolveCorrelationId() {
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        return UUID.randomUUID().toString();
    }
}
