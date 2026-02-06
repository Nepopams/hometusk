package com.hometusk.shopping.dto;

import com.hometusk.shopping.domain.ShoppingRun;
import com.hometusk.tasks.dto.UserSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Full shopping run with items")
public record ShoppingRunDto(
        @Schema(description = "Run ID") UUID id,
        @Schema(description = "Household ID") UUID householdId,
        @Schema(description = "Source list ID") UUID listId,
        @Schema(description = "List name snapshot") String listName,
        @Schema(description = "Run status") String status,
        @Schema(description = "Created by") UserSummaryDto createdBy,
        @Schema(description = "Created at") Instant createdAt,
        @Schema(description = "Closed at") Instant closedAt,
        @Schema(description = "Items") List<ShoppingRunItemDto> items,
        @Schema(description = "Item counts") ItemCountsDto itemCounts) {

    public static ShoppingRunDto from(ShoppingRun run) {
        List<ShoppingRunItemDto> items = run.getItems().stream().map(ShoppingRunItemDto::from).toList();
        return new ShoppingRunDto(
                run.getId(),
                run.getHouseholdId(),
                run.getSourceListId(),
                run.getListName(),
                run.getStatus().name(),
                UserSummaryDto.from(run.getCreatedBy()),
                run.getCreatedAt(),
                run.getClosedAt(),
                items,
                ItemCountsDto.from(run.getItemCounts()));
    }
}
