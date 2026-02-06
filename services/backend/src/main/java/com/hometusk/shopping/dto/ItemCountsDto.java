package com.hometusk.shopping.dto;

import com.hometusk.shopping.domain.ItemCounts;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Item count summary")
public record ItemCountsDto(
        @Schema(description = "Total items") int total,
        @Schema(description = "Purchased items") int purchased,
        @Schema(description = "Remaining items") int remaining) {

    public static ItemCountsDto from(int total, int purchased) {
        return new ItemCountsDto(total, purchased, total - purchased);
    }

    public static ItemCountsDto from(ItemCounts counts) {
        return new ItemCountsDto(counts.total(), counts.purchased(), counts.remaining());
    }
}
