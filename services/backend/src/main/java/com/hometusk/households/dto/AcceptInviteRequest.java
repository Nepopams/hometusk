package com.hometusk.households.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Accept household invite request")
public record AcceptInviteRequest(
        @Schema(description = "Invite token", example = "hti_xxx") @NotBlank(message = "inviteToken is required")
                String inviteToken) {}
