package com.hometusk.shopping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update a shopping item")
public record UpdateShoppingItemRequest(
        @Schema(description = "Whether the item has been purchased", example = "true")
                @NotNull(message = "Purchased status is required")
                Boolean purchased) {}
