package com.hometusk.shopping.dto;

import com.hometusk.shopping.domain.ShoppingRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to close a shopping run")
public record CloseShoppingRunRequest(
        @NotNull @Schema(description = "Final status (COMPLETED or CANCELLED)") ShoppingRunStatus status) {}
