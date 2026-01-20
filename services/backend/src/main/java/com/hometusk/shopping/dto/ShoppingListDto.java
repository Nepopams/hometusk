package com.hometusk.shopping.dto;

import com.hometusk.shopping.domain.ShoppingList;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Shopping list")
public record ShoppingListDto(
        @Schema(description = "List ID") UUID id,
        @Schema(description = "List name") String name,
        @Schema(description = "Household ID") UUID householdId,
        @Schema(description = "Count of unpurchased items") long unpurchasedCount,
        @Schema(description = "Creation timestamp") Instant createdAt) {

    public static ShoppingListDto from(ShoppingList list, long unpurchasedCount) {
        return new ShoppingListDto(
                list.getId(), list.getName(), list.getHouseholdId(), unpurchasedCount, list.getCreatedAt());
    }
}
