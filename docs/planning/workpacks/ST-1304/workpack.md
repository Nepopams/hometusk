# Workpack: ST-1304 — Marketplace Link-out Templates + Config

## Sources of Truth
- Product Goal: `docs/planning/strategy/product-goal.md`
- Scope Anchor: `docs/planning/releases/MVP.md`
- Epic: `docs/planning/epics/EP-013/epic.md`
- Story: `docs/planning/epics/EP-013/stories/ST-1304-marketplace-linkouts.md`
- Contract: `docs/contracts/http/shopping-marketplaces.openapi.yaml` (lines 321-354)
- ADR: `docs/adr/015-marketplace-linkout-encoding.md`
- DoR: `docs/_governance/dor.md`
- DoD: `docs/_governance/dod.md`

---

## Status
**Ready** — ADR-015 approved 2026-02-03

---

## Goal
Add `GET /api/v1/marketplace-templates` endpoint returning configured marketplace templates for link-out URL generation. Templates are configured via application.yml and validated at startup.

---

## Scope

### In Scope
- MarketplaceProperties (ConfigurationProperties)
- MarketplaceTemplate configuration POJO
- MarketplaceConfigService with template validation
- MarketplaceController with public endpoint
- MarketplaceTemplateDto
- Configuration in application.yml (Ozon + Yandex Market)
- Startup validation (no javascript:, data: schemes, required {query} placeholder)
- Unit tests for service logic
- Integration tests for endpoint

### Out of Scope
- Per-household marketplace config
- Database-stored templates
- URL generation on server side (client responsibility per ADR-015)
- Affiliate links/tracking

---

## Files to Create/Modify

| Path | Action | Purpose |
|------|--------|---------|
| `services/backend/src/main/java/com/hometusk/marketplace/MarketplaceProperties.java` | CREATE | ConfigurationProperties for templates |
| `services/backend/src/main/java/com/hometusk/marketplace/MarketplaceTemplate.java` | CREATE | Template POJO |
| `services/backend/src/main/java/com/hometusk/marketplace/MarketplaceConfigService.java` | CREATE | Service with validation |
| `services/backend/src/main/java/com/hometusk/marketplace/api/MarketplaceController.java` | CREATE | REST endpoint |
| `services/backend/src/main/java/com/hometusk/marketplace/dto/MarketplaceTemplateDto.java` | CREATE | Response DTO |
| `services/backend/src/main/resources/application.yml` | MODIFY | Add marketplace templates config |
| `services/backend/src/test/java/com/hometusk/marketplace/MarketplaceConfigServiceTest.java` | CREATE | Unit tests |
| `services/backend/src/test/java/com/hometusk/integration/marketplace/MarketplaceIntegrationTest.java` | CREATE | Integration tests |

---

## Implementation Plan

### Step 1: Create MarketplaceTemplate POJO

**File:** `services/backend/src/main/java/com/hometusk/marketplace/MarketplaceTemplate.java`

```java
public class MarketplaceTemplate {
    private String id;
    private String name;
    private String urlTemplate;
    private String iconUrl;
    private boolean enabled = true;
    // getters/setters
}
```

### Step 2: Create MarketplaceProperties

**File:** `services/backend/src/main/java/com/hometusk/marketplace/MarketplaceProperties.java`

```java
@ConfigurationProperties(prefix = "hometusk.marketplaces")
public class MarketplaceProperties {
    private List<MarketplaceTemplate> templates = new ArrayList<>();
    // getter/setter
}
```

Enable in main application class with `@EnableConfigurationProperties(MarketplaceProperties.class)`.

### Step 3: Create MarketplaceTemplateDto

**File:** `services/backend/src/main/java/com/hometusk/marketplace/dto/MarketplaceTemplateDto.java`

```java
public record MarketplaceTemplateDto(
    String id,
    String name,
    String urlTemplate,
    String iconUrl
) {
    public static MarketplaceTemplateDto from(MarketplaceTemplate t) {
        return new MarketplaceTemplateDto(t.getId(), t.getName(), t.getUrlTemplate(), t.getIconUrl());
    }
}
```

### Step 4: Create MarketplaceConfigService

**File:** `services/backend/src/main/java/com/hometusk/marketplace/MarketplaceConfigService.java`

**Key elements:**
- `@PostConstruct` validation of all templates
- `getEnabledTemplates()` returns only enabled templates
- Validation rules per ADR-015:
  - Template must contain exactly one `{query}` placeholder
  - No `javascript:` or `data:` schemes
  - id, name, urlTemplate must not be blank

```java
@Service
public class MarketplaceConfigService {
    private final MarketplaceProperties properties;

    @PostConstruct
    void validateTemplates() {
        for (MarketplaceTemplate t : properties.getTemplates()) {
            validateTemplate(t);
        }
    }

    public List<MarketplaceTemplateDto> getEnabledTemplates() {
        return properties.getTemplates().stream()
            .filter(MarketplaceTemplate::isEnabled)
            .map(MarketplaceTemplateDto::from)
            .toList();
    }

    private void validateTemplate(MarketplaceTemplate t) {
        // validation logic
    }
}
```

### Step 5: Create MarketplaceController

**File:** `services/backend/src/main/java/com/hometusk/marketplace/api/MarketplaceController.java`

```java
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Marketplaces")
public class MarketplaceController {

    private final MarketplaceConfigService configService;

    @GetMapping("/marketplace-templates")
    public ResponseEntity<List<MarketplaceTemplateDto>> getMarketplaceTemplates() {
        return ResponseEntity.ok(configService.getEnabledTemplates());
    }
}
```

**Note:** Endpoint is public (no auth) per contract `security: []`.

### Step 6: Add Configuration to application.yml

```yaml
hometusk:
  marketplaces:
    templates:
      - id: ozon
        name: Ozon
        urlTemplate: "https://www.ozon.ru/search/?text={query}"
        iconUrl: /icons/ozon.svg
        enabled: true
      - id: yandex_market
        name: Yandex Market
        urlTemplate: "https://market.yandex.ru/search?text={query}"
        iconUrl: /icons/yandex-market.svg
        enabled: true
```

### Step 7: Write Unit Tests

**File:** `services/backend/src/test/java/com/hometusk/marketplace/MarketplaceConfigServiceTest.java`

**Test cases:**
- `getEnabledTemplates_returnsOnlyEnabled`
- `getEnabledTemplates_mapsToDto`
- `validateTemplates_validTemplate_noException`
- `validateTemplates_missingQueryPlaceholder_throwsException`
- `validateTemplates_javascriptScheme_throwsException`
- `validateTemplates_dataScheme_throwsException`
- `validateTemplates_blankId_throwsException`
- `validateTemplates_multipleQueryPlaceholders_throwsException`

### Step 8: Write Integration Tests

**File:** `services/backend/src/test/java/com/hometusk/integration/marketplace/MarketplaceIntegrationTest.java`

**Test cases:**
- `getMarketplaceTemplates_returnsConfiguredTemplates`
- `getMarketplaceTemplates_noAuth_returns200` (public endpoint)
- `getMarketplaceTemplates_responseStructure_matchesContract`

---

## Verification Commands

```bash
cd /home/vad/Документы/hometusk/services/backend

# Build
./gradlew build

# Spotless
./gradlew spotlessCheck
./gradlew spotlessApply

# Unit tests
./gradlew test --tests "*MarketplaceConfigServiceTest*"

# Integration tests
./gradlew test --tests "*MarketplaceIntegrationTest*"

# All tests
./gradlew test
```

---

## Acceptance Criteria Mapping

| AC | Criteria | Verification |
|----|----------|--------------|
| AC-1 | Template config returned | Integration test |
| AC-2 | URL encoding (client side) | N/A - documented in ADR |
| AC-3 | XSS prevention | Unit test for validation |
| AC-4 | Disabled templates filtered | Unit test |
| AC-5 | Missing placeholder handled | Unit test for validation |

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Invalid template in config | HIGH | @PostConstruct validation fails startup |
| Missing test coverage for edge cases | LOW | Comprehensive unit tests |

---

## Rollback

- Revert code changes (no migration required)
- Remove config from application.yml

---

## References

- Contract: Shopping Marketplaces OpenAPI (marketplace-templates)
- ADR-015: Marketplace link-out safe encoding
- Patterns: ShoppingController.java (existing endpoints)
