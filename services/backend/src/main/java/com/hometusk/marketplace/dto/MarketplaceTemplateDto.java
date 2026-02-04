package com.hometusk.marketplace.dto;

import com.hometusk.marketplace.MarketplaceTemplate;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Marketplace configuration for link-out URLs")
public record MarketplaceTemplateDto(
        @Schema(description = "Unique identifier", example = "ozon") String id,
        @Schema(description = "Display name", example = "Ozon") String name,
        @Schema(
                        description = "URL template with {query} placeholder",
                        example = "https://www.ozon.ru/search/?text={query}")
                String urlTemplate,
        @Schema(description = "Icon URL for UI display", example = "/icons/ozon.svg") String iconUrl) {

    public static MarketplaceTemplateDto from(MarketplaceTemplate template) {
        return new MarketplaceTemplateDto(
                template.getId(), template.getName(), template.getUrlTemplate(), template.getIconUrl());
    }
}
