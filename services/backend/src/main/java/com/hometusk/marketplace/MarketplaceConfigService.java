package com.hometusk.marketplace;

import com.hometusk.marketplace.dto.MarketplaceTemplateDto;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarketplaceConfigService {

    private static final Logger log = LoggerFactory.getLogger(MarketplaceConfigService.class);
    private static final String QUERY_PLACEHOLDER = "{query}";

    private final MarketplaceProperties properties;

    public MarketplaceConfigService(MarketplaceProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validateTemplates() {
        log.info(
                "Validating {} marketplace templates", properties.getTemplates().size());
        for (MarketplaceTemplate template : properties.getTemplates()) {
            validateTemplate(template);
        }
        log.info("All marketplace templates validated successfully");
    }

    public List<MarketplaceTemplateDto> getEnabledTemplates() {
        return properties.getTemplates().stream()
                .filter(MarketplaceTemplate::isEnabled)
                .map(MarketplaceTemplateDto::from)
                .toList();
    }

    private void validateTemplate(MarketplaceTemplate template) {
        if (template.getId() == null || template.getId().isBlank()) {
            throw new IllegalStateException("Marketplace template id must not be blank");
        }
        if (template.getName() == null || template.getName().isBlank()) {
            throw new IllegalStateException("Marketplace template name must not be blank for id: " + template.getId());
        }
        if (template.getUrlTemplate() == null || template.getUrlTemplate().isBlank()) {
            throw new IllegalStateException(
                    "Marketplace template urlTemplate must not be blank for id: " + template.getId());
        }

        String url = template.getUrlTemplate().toLowerCase();

        if (url.startsWith("javascript:")) {
            throw new IllegalStateException(
                    "Marketplace template urlTemplate must not use javascript: scheme for id: " + template.getId());
        }
        if (url.startsWith("data:")) {
            throw new IllegalStateException(
                    "Marketplace template urlTemplate must not use data: scheme for id: " + template.getId());
        }

        int count = countOccurrences(template.getUrlTemplate(), QUERY_PLACEHOLDER);
        if (count == 0) {
            throw new IllegalStateException(
                    "Marketplace template urlTemplate must contain {query} placeholder for id: " + template.getId());
        }
        if (count > 1) {
            throw new IllegalStateException(
                    "Marketplace template urlTemplate must contain exactly one {query} placeholder for id: "
                            + template.getId());
        }

        log.debug("Validated marketplace template: {}", template.getId());
    }

    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
