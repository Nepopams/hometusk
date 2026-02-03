package com.hometusk.asr.controller;

import com.hometusk.asr.dto.CreateTranscriptionResponse;
import com.hometusk.asr.dto.TranscriptionResultResponse;
import com.hometusk.asr.service.AsrRateLimitService;
import com.hometusk.asr.service.AsrService;
import com.hometusk.asr.service.AsrValidationService;
import com.hometusk.shared.logging.MdcKeys;
import com.hometusk.shared.security.CurrentUser;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/households/{householdId}/asr")
@Tag(name = "ASR", description = "Speech-to-text transcription endpoints")
public class AsrController {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    private final AsrService asrService;
    private final AsrValidationService validationService;
    private final AsrRateLimitService rateLimitService;
    private final MembershipService membershipService;
    private final UserResolver userResolver;

    public AsrController(
            AsrService asrService,
            AsrValidationService validationService,
            AsrRateLimitService rateLimitService,
            MembershipService membershipService,
            UserResolver userResolver) {
        this.asrService = asrService;
        this.validationService = validationService;
        this.rateLimitService = rateLimitService;
        this.membershipService = membershipService;
        this.userResolver = userResolver;
    }

    @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a transcription job")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Transcription job created"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<CreateTranscriptionResponse> createTranscription(
            @PathVariable UUID householdId,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "languageHint", defaultValue = "auto") String languageHint,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey)
            throws IOException {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        MDC.put(MdcKeys.HOUSEHOLD_ID, householdId.toString());
        membershipService.requireMembership(currentUser.id(), householdId);

        rateLimitService.checkPostLimit(householdId, currentUser.id());
        validationService.validateFile(file);

        String correlationId = resolveCorrelationId();
        byte[] fileBytes = file.getBytes();
        CreateTranscriptionResponse response = asrService.createTranscription(
                householdId,
                currentUser.id(),
                fileBytes,
                file.getOriginalFilename(),
                file.getContentType(),
                languageHint,
                correlationId,
                idempotencyKey);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .header(CORRELATION_ID_HEADER, correlationId)
                .body(response);
    }

    @GetMapping("/transcriptions/{transcriptionId}")
    @Operation(summary = "Get transcription status and result")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transcription found"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household"),
        @ApiResponse(responseCode = "404", description = "Transcription not found")
    })
    public ResponseEntity<TranscriptionResultResponse> getTranscription(
            @PathVariable UUID householdId, @PathVariable UUID transcriptionId) {
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        MDC.put(MdcKeys.HOUSEHOLD_ID, householdId.toString());
        membershipService.requireMembership(currentUser.id(), householdId);

        rateLimitService.checkGetLimit(householdId, currentUser.id());
        String correlationId = resolveCorrelationId();
        TranscriptionResultResponse response = asrService.getTranscription(householdId, transcriptionId, correlationId);

        return ResponseEntity.ok().header(CORRELATION_ID_HEADER, correlationId).body(response);
    }

    private String resolveCorrelationId() {
        String correlationId = MDC.get(MdcKeys.CORRELATION_ID);
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        return UUID.randomUUID().toString();
    }
}
