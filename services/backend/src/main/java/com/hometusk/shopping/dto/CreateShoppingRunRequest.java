package com.hometusk.shopping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request to create a shopping run")
public record CreateShoppingRunRequest(
        @NotNull @Schema(description = "Shopping list ID to create run from") UUID listId) {}
