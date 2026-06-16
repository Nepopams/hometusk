package com.hometusk.commands.dto;

import jakarta.validation.constraints.Size;

public record CommandConfirmationCancelRequest(
        @Size(max = 500, message = "reason must be at most 500 characters") String reason) {}
