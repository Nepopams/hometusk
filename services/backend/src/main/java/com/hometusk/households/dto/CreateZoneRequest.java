package com.hometusk.households.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a new zone")
public record CreateZoneRequest(
        @Schema(description = "Zone name", example = "Kitchen")
                @NotBlank(message = "Name is required")
                @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
                String name) {

    /**
     * Returns the name with leading/trailing whitespace trimmed.
     */
    public String trimmedName() {
        return name != null ? name.trim() : null;
    }
}
