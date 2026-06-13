package com.hometusk.shopping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a shopping list")
public record CreateShoppingListRequest(
        @Schema(description = "Shopping list name", example = "Groceries")
                @NotBlank(message = "Name is required")
                @Size(min = 1, max = 80, message = "Name must be between 1 and 80 characters")
                String name) {

    public String trimmedName() {
        return name != null ? name.trim() : null;
    }
}
