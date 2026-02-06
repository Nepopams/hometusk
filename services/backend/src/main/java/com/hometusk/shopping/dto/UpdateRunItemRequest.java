package com.hometusk.shopping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update a run item")
public record UpdateRunItemRequest(
        @NotNull @Schema(description = "Whether item is purchased") Boolean purchased,
        @Schema(description = "Sync purchase status to original list item (default true)") Boolean syncToList) {

    public boolean shouldSyncToList() {
        return syncToList == null || syncToList;
    }
}
