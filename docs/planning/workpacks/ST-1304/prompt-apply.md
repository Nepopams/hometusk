# Codex APPLY Prompt — ST-1304: Marketplace Link-out Templates

## Directive
**IMPLEMENTATION phase.** Create files as specified. Run verification commands. Stop on failure.

---

## Context

**Story:** ST-1304 — Marketplace Link-out Templates + Config
**Points:** 5
**Epic:** EP-013 (Shopping Marketplaces)

**Goal:** Add `GET /api/v1/marketplace-templates` public endpoint returning configured marketplace templates.

---

## Sources of Truth

```
docs/planning/workpacks/ST-1304/workpack.md
docs/contracts/http/shopping-marketplaces.openapi.yaml (lines 321-354)
docs/adr/015-marketplace-linkout-encoding.md
```

---

## PLAN Findings Summary

1. **ConfigurationProperties:** Use @ConfigurationProperties + @Configuration (like GuardrailsConfig)
2. **Controller:** @RestController, @Tag, @Operation, ResponseEntity.ok()
3. **DTO:** Java record with @Schema, static `from()` factory
4. **Security:** Must add `/api/v1/marketplace-templates` to SecurityConfig permitAll
5. **Validation:** Use IllegalStateException for startup failures (no existing @PostConstruct pattern)
6. **Config prefix:** `hometusk.marketplaces` with kebab-case in YAML

---

## Implementation Steps

### Step 1: Create MarketplaceTemplate POJO

**File:** `services/backend/src/main/java/com/hometusk/marketplace/MarketplaceTemplate.java`

```java
package com.hometusk.marketplace;

public class MarketplaceTemplate {

    private String id;
    private String name;
    private String urlTemplate;
    private String iconUrl;
    private boolean enabled = true;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
```

### Step 2: Create MarketplaceProperties

**File:** `services/backend/src/main/java/com/hometusk/marketplace/MarketplaceProperties.java`

```java
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
```

### Step 3: Create MarketplaceTemplateDto

**File:** `services/backend/src/main/java/com/hometusk/marketplace/dto/MarketplaceTemplateDto.java`

```java
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
```

### Step 4: Create MarketplaceConfigService

**File:** `services/backend/src/main/java/com/hometusk/marketplace/MarketplaceConfigService.java`

```java
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
        log.info("Validating {} marketplace templates", properties.getTemplates().size());
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
            throw new IllegalStateException(
                    "Marketplace template name must not be blank for id: " + template.getId());
        }
        if (template.getUrlTemplate() == null || template.getUrlTemplate().isBlank()) {
            throw new IllegalStateException(
                    "Marketplace template urlTemplate must not be blank for id: " + template.getId());
        }

        String url = template.getUrlTemplate().toLowerCase();

        // Security: no javascript or data schemes
        if (url.startsWith("javascript:")) {
            throw new IllegalStateException(
                    "Marketplace template urlTemplate must not use javascript: scheme for id: " + template.getId());
        }
        if (url.startsWith("data:")) {
            throw new IllegalStateException(
                    "Marketplace template urlTemplate must not use data: scheme for id: " + template.getId());
        }

        // Must contain exactly one {query} placeholder
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
```

### Step 5: Create MarketplaceController

**File:** `services/backend/src/main/java/com/hometusk/marketplace/api/MarketplaceController.java`

```java
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
```

### Step 6: Update SecurityConfig

**File:** `services/backend/src/main/java/com/hometusk/config/SecurityConfig.java`

**Modify:** Add `/api/v1/marketplace-templates` to permitAll list (line ~54):

```java
.requestMatchers(
        "/actuator/health",
        "/actuator/info",
        "/actuator/metrics",
        "/actuator/metrics/**",
        "/actuator/prometheus",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/api-docs/**",
        "/v3/api-docs/**",
        "/api/v1/marketplace-templates")  // ADD THIS LINE
.permitAll()
```

### Step 7: Add Configuration to application.yml

**File:** `services/backend/src/main/resources/application.yml`

**Add** at the end of the `hometusk:` section (or create if not exists):

```yaml
hometusk:
  # ... existing config ...
  marketplaces:
    templates:
      - id: ozon
        name: Ozon
        url-template: "https://www.ozon.ru/search/?text={query}"
        icon-url: /icons/ozon.svg
        enabled: true
      - id: yandex_market
        name: Yandex Market
        url-template: "https://market.yandex.ru/search?text={query}"
        icon-url: /icons/yandex-market.svg
        enabled: true
```

**Note:** Use kebab-case (`url-template`, `icon-url`) in YAML - Spring Boot maps to camelCase fields.

### Step 8: Create Unit Tests

**File:** `services/backend/src/test/java/com/hometusk/marketplace/MarketplaceConfigServiceTest.java`

```java
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

        service.validateTemplates(); // should not throw
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
```

### Step 9: Create Integration Tests

**File:** `services/backend/src/test/java/com/hometusk/integration/marketplace/MarketplaceIntegrationTest.java`

```java
package com.hometusk.integration.marketplace;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hometusk.integration.IntegrationTestBase;
import org.junit.jupiter.api.Test;

class MarketplaceIntegrationTest extends IntegrationTestBase {

    @Test
    void getMarketplaceTemplates_returnsConfiguredTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is("ozon")))
                .andExpect(jsonPath("$[0].name", is("Ozon")))
                .andExpect(jsonPath("$[0].urlTemplate").exists())
                .andExpect(jsonPath("$[1].id", is("yandex_market")));
    }

    @Test
    void getMarketplaceTemplates_noAuth_returns200() throws Exception {
        // No jwt() - should still work because endpoint is public
        mockMvc.perform(get("/api/v1/marketplace-templates")).andExpect(status().isOk());
    }

    @Test
    void getMarketplaceTemplates_responseStructure_matchesContract() throws Exception {
        mockMvc.perform(get("/api/v1/marketplace-templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").isString())
                .andExpect(jsonPath("$[0].name").isString())
                .andExpect(jsonPath("$[0].urlTemplate").isString())
                .andExpect(jsonPath("$[0].iconUrl").exists());
    }
}
```

---

## Verification Commands

Run in order, stop on first failure:

```bash
cd /home/vad/Документы/hometusk/services/backend

# 1. Spotless
./gradlew spotlessApply

# 2. Build
./gradlew build

# 3. Unit tests
./gradlew test --tests "*MarketplaceConfigServiceTest*"

# 4. Integration tests
./gradlew test --tests "*MarketplaceIntegrationTest*"

# 5. Full test suite
./gradlew test
```

---

## Files Checklist

| File | Action |
|------|--------|
| `src/main/java/com/hometusk/marketplace/MarketplaceTemplate.java` | CREATE |
| `src/main/java/com/hometusk/marketplace/MarketplaceProperties.java` | CREATE |
| `src/main/java/com/hometusk/marketplace/dto/MarketplaceTemplateDto.java` | CREATE |
| `src/main/java/com/hometusk/marketplace/MarketplaceConfigService.java` | CREATE |
| `src/main/java/com/hometusk/marketplace/api/MarketplaceController.java` | CREATE |
| `src/main/java/com/hometusk/config/SecurityConfig.java` | MODIFY |
| `src/main/resources/application.yml` | MODIFY |
| `src/test/java/com/hometusk/marketplace/MarketplaceConfigServiceTest.java` | CREATE |
| `src/test/java/com/hometusk/integration/marketplace/MarketplaceIntegrationTest.java` | CREATE |

---

## STOP Conditions

- Stop if any verification command fails
- Stop if you need to deviate from the plan
- Do NOT add features beyond the scope
