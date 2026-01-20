package com.hometusk.shopping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to add a shopping item")
public record AddShoppingItemRequest(
        @Schema(description = "Item name", example = "Milk")
                @NotBlank(message = "Name is required")
                @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
                String name,
        @Schema(description = "Quantity", example = "2") @Min(value = 1, message = "Quantity must be at least 1")
                Integer quantity,
        @Schema(description = "Unit of measurement", example = "liters")
                @Size(max = 50, message = "Unit must be at most 50 characters")
                String unit) {

    /**
     * Returns the quantity, defaulting to 1 if not specified.
     */
    public int resolvedQuantity() {
        return quantity != null ? quantity : 1;
    }
}
