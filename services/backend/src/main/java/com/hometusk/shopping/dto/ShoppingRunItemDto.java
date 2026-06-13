package com.hometusk.shopping.dto;

import com.hometusk.shopping.domain.ShoppingRunItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Item snapshot within a shopping run")
public record ShoppingRunItemDto(
        @Schema(description = "Run item ID") UUID id,
        @Schema(description = "Original ShoppingItem ID") UUID originalItemId,
        @Schema(description = "Item name") String name,
        @Schema(description = "Quantity") Integer quantity,
        @Schema(description = "Unit") String unit,
        @Schema(description = "Optional shopping category snapshot") String category,
        @Schema(description = "Optional source/store snapshot") String source,
        @Schema(description = "Purchased in this run") boolean purchased,
        @Schema(description = "When purchased") Instant purchasedAt) {

    public static ShoppingRunItemDto from(ShoppingRunItem item) {
        return new ShoppingRunItemDto(
                item.getId(),
                item.getOriginalItemId(),
                item.getName(),
                item.getQuantity(),
                item.getUnit(),
                item.getCategory(),
                item.getSource(),
                item.isPurchased(),
                item.getPurchasedAt());
    }
}
