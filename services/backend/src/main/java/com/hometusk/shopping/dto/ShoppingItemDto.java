package com.hometusk.shopping.dto;

import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.tasks.dto.UserSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Shopping item")
public record ShoppingItemDto(
        @Schema(description = "Item ID") UUID id,
        @Schema(description = "Item name") String name,
        @Schema(description = "Quantity") Integer quantity,
        @Schema(description = "Unit of measurement") String unit,
        @Schema(description = "Whether item has been purchased") boolean purchased,
        @Schema(description = "Linked task ID (if any)") UUID linkedTaskId,
        @Schema(description = "User who added the item") UserSummaryDto addedBy,
        @Schema(description = "Creation timestamp") Instant createdAt,
        @Schema(description = "Purchase timestamp") Instant purchasedAt) {

    public static ShoppingItemDto from(ShoppingItem item) {
        return new ShoppingItemDto(
                item.getId(),
                item.getName(),
                item.getQuantity(),
                item.getUnit(),
                item.isPurchased(),
                item.getLinkedTaskId(),
                UserSummaryDto.from(item.getAddedBy()),
                item.getCreatedAt(),
                item.getPurchasedAt());
    }
}
