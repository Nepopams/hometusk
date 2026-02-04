package com.hometusk.marketplace;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hometusk.marketplaces")
public class MarketplaceProperties {

    private List<MarketplaceTemplate> templates = new ArrayList<>();

    public List<MarketplaceTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(List<MarketplaceTemplate> templates) {
        this.templates = templates;
    }
}
