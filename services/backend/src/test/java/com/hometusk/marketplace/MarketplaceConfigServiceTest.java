package com.hometusk.marketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hometusk.marketplace.dto.MarketplaceTemplateDto;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketplaceConfigServiceTest {

    private MarketplaceProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MarketplaceProperties();
    }

    @Test
    void getEnabledTemplates_returnsOnlyEnabled() {
        MarketplaceTemplate enabled = createTemplate("ozon", "Ozon", "https://ozon.ru?text={query}", true);
        MarketplaceTemplate disabled = createTemplate("disabled", "Disabled", "https://example.com?q={query}", false);
        properties.setTemplates(List.of(enabled, disabled));

        MarketplaceConfigService service = new MarketplaceConfigService(properties);

        List<MarketplaceTemplateDto> result = service.getEnabledTemplates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("ozon");
    }

    @Test
    void getEnabledTemplates_mapsToDto() {
        MarketplaceTemplate template = createTemplate("ozon", "Ozon", "https://ozon.ru?text={query}", true);
        template.setIconUrl("/icons/ozon.svg");
        properties.setTemplates(List.of(template));

        MarketplaceConfigService service = new MarketplaceConfigService(properties);

        List<MarketplaceTemplateDto> result = service.getEnabledTemplates();

        assertThat(result).hasSize(1);
        MarketplaceTemplateDto dto = result.get(0);
        assertThat(dto.id()).isEqualTo("ozon");
        assertThat(dto.name()).isEqualTo("Ozon");
        assertThat(dto.urlTemplate()).isEqualTo("https://ozon.ru?text={query}");
        assertThat(dto.iconUrl()).isEqualTo("/icons/ozon.svg");
    }

    @Test
    void validateTemplates_validTemplate_noException() {
        MarketplaceTemplate template = createTemplate("ozon", "Ozon", "https://ozon.ru?text={query}", true);
        properties.setTemplates(List.of(template));

        MarketplaceConfigService service = new MarketplaceConfigService(properties);

        service.validateTemplates();
    }

    @Test
    void validateTemplates_missingQueryPlaceholder_throwsException() {
        MarketplaceTemplate template = createTemplate("bad", "Bad", "https://example.com/search", true);
        properties.setTemplates(List.of(template));

        MarketplaceConfigService service = new MarketplaceConfigService(properties);

        assertThatThrownBy(service::validateTemplates)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("{query}");
    }

    @Test
    void validateTemplates_javascriptScheme_throwsException() {
        MarketplaceTemplate template = createTemplate("xss", "XSS", "javascript:alert({query})", true);
        properties.setTemplates(List.of(template));

        MarketplaceConfigService service = new MarketplaceConfigService(properties);

        assertThatThrownBy(service::validateTemplates)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("javascript:");
    }

    @Test
    void validateTemplates_dataScheme_throwsException() {
        MarketplaceTemplate template = createTemplate("data", "Data", "data:text/html,{query}", true);
        properties.setTemplates(List.of(template));

        MarketplaceConfigService service = new MarketplaceConfigService(properties);

        assertThatThrownBy(service::validateTemplates)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("data:");
    }

    @Test
    void validateTemplates_blankId_throwsException() {
        MarketplaceTemplate template = createTemplate("", "Name", "https://example.com?q={query}", true);
        properties.setTemplates(List.of(template));

        MarketplaceConfigService service = new MarketplaceConfigService(properties);

        assertThatThrownBy(service::validateTemplates)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("id must not be blank");
    }

    @Test
    void validateTemplates_blankName_throwsException() {
        MarketplaceTemplate template = createTemplate("id", "", "https://example.com?q={query}", true);
        properties.setTemplates(List.of(template));

        MarketplaceConfigService service = new MarketplaceConfigService(properties);

        assertThatThrownBy(service::validateTemplates)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("name must not be blank");
    }

    @Test
    void validateTemplates_multipleQueryPlaceholders_throwsException() {
        MarketplaceTemplate template = createTemplate("multi", "Multi", "https://ex.com?q={query}&q2={query}", true);
        properties.setTemplates(List.of(template));

        MarketplaceConfigService service = new MarketplaceConfigService(properties);

        assertThatThrownBy(service::validateTemplates)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one");
    }

    private MarketplaceTemplate createTemplate(String id, String name, String urlTemplate, boolean enabled) {
        MarketplaceTemplate t = new MarketplaceTemplate();
        t.setId(id);
        t.setName(name);
        t.setUrlTemplate(urlTemplate);
        t.setEnabled(enabled);
        return t;
    }
}
