package com.hometusk.marketplace.api;

import com.hometusk.marketplace.MarketplaceConfigService;
import com.hometusk.marketplace.dto.MarketplaceTemplateDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Marketplaces", description = "Marketplace template configuration")
public class MarketplaceController {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceController.class);

    private final MarketplaceConfigService configService;

    public MarketplaceController(MarketplaceConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/marketplace-templates")
    @Operation(
            summary = "Get available marketplace templates",
            description =
                    """
            Returns list of configured marketplace templates for link-out generation.
            Only enabled templates are returned.
            Note: This endpoint does not require authentication.
            """)
    @ApiResponse(responseCode = "200", description = "List of marketplace templates")
    public ResponseEntity<List<MarketplaceTemplateDto>> getMarketplaceTemplates() {
        log.debug("Getting marketplace templates");
        List<MarketplaceTemplateDto> templates = configService.getEnabledTemplates();
        log.debug("Returning {} marketplace templates", templates.size());
        return ResponseEntity.ok(templates);
    }
}
