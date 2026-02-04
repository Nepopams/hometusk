# Codex PLAN Prompt — ST-1304: Marketplace Link-out Templates

## Directive
**READ-ONLY exploration.** Do NOT edit or create any files. Your output will inform the APPLY phase.

---

## Context

**Story:** ST-1304 — Marketplace Link-out Templates + Config
**Points:** 5
**Epic:** EP-013 (Shopping Marketplaces)

**Goal:** Add `GET /api/v1/marketplace-templates` public endpoint returning configured marketplace templates from application.yml.

---

## Sources of Truth (READ these files)

```
docs/planning/workpacks/ST-1304/workpack.md              # Implementation plan
docs/contracts/http/shopping-marketplaces.openapi.yaml   # Contract (lines 321-354)
docs/adr/015-marketplace-linkout-encoding.md             # Encoding decisions
docs/planning/epics/EP-013/stories/ST-1304-marketplace-linkouts.md  # Story spec
```

---

## Exploration Tasks

### 1. Examine existing controller patterns
```bash
cat services/backend/src/main/java/com/hometusk/shopping/api/ShoppingController.java
```
**Find:**
- How controllers are structured (annotations, constructor injection)
- ResponseEntity patterns
- Swagger/OpenAPI annotations (@Tag, @Operation, @ApiResponses)

### 2. Examine existing ConfigurationProperties patterns
```bash
rg -l "ConfigurationProperties" services/backend/src/main/java --type java
cat services/backend/src/main/java/com/hometusk/config/AppProperties.java 2>/dev/null || echo "not found"
rg "@ConfigurationProperties" services/backend/src/main/java --type java -A 5
```
**Find:**
- How ConfigurationProperties classes are structured
- Where @EnableConfigurationProperties is used
- Prefix naming conventions

### 3. Examine application.yml structure
```bash
cat services/backend/src/main/resources/application.yml
```
**Find:**
- Existing config sections
- Naming conventions (kebab-case vs camelCase)
- Where to add new `hometusk.marketplaces` section

### 4. Examine DTO record patterns
```bash
cat services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingItemDto.java
cat services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingListDto.java
```
**Find:**
- Record structure conventions
- Static factory method patterns (`from()`)

### 5. Examine integration test patterns
```bash
cat services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingExportIntegrationTest.java
```
**Find:**
- Test class structure
- MockMvc usage for public endpoints (without auth)
- How to test configuration-based endpoints

### 6. Check for existing validation patterns at startup
```bash
rg "@PostConstruct" services/backend/src/main/java --type java -A 10
```
**Find:**
- How startup validation is implemented
- Exception types used for config validation

### 7. Verify contract details
```bash
sed -n '321,355p' docs/contracts/http/shopping-marketplaces.openapi.yaml
```
**Confirm:**
- Endpoint path: `/marketplace-templates`
- No authentication required (`security: []`)
- Response schema: array of MarketplaceTemplateDto

---

## Expected Findings to Report

After exploration, provide:

1. **ConfigurationProperties pattern:**
   - Existing class to use as reference
   - How to enable in main application class

2. **Controller pattern:**
   - Base annotations
   - How to mark endpoint as public (no auth)

3. **DTO pattern:**
   - Record vs class decision
   - Factory method convention

4. **Test pattern:**
   - Base class to extend
   - How to call public endpoint without jwt()

5. **Validation pattern:**
   - Exception type for invalid config
   - @PostConstruct usage

6. **Confirmed paths:**
   - Package: `com.hometusk.marketplace`
   - Controller: `com.hometusk.marketplace.api.MarketplaceController`
   - Service: `com.hometusk.marketplace.MarketplaceConfigService`

---

## Allowed Commands (whitelist)

- `ls`, `find` (directory exploration)
- `cat`, `head`, `tail` (file reading)
- `rg`, `grep` (search)
- `sed -n` (extract lines)
- `git status`, `git diff` (read-only inspection)

## Forbidden

- File modifications (edit/write/move/delete)
- `./gradlew` commands
- Any network access
- Package installations

---

## Output Format

```markdown
## PLAN Findings for ST-1304

### 1. ConfigurationProperties Pattern
- Reference class: `ClassName`
- Enabled via: description
- Prefix convention: description

### 2. Controller Pattern
- Annotations: list
- Public endpoint: how to configure

### 3. DTO Pattern
- Style: record / class
- Factory method: `from(Entity)`

### 4. Test Pattern
- Base class: `ClassName`
- Public endpoint test: description

### 5. Validation Pattern
- Exception type: `ClassName`
- @PostConstruct usage: description

### 6. Confirmed Paths
- Properties: `exact/path/MarketplaceProperties.java`
- Template: `exact/path/MarketplaceTemplate.java`
- Service: `exact/path/MarketplaceConfigService.java`
- Controller: `exact/path/MarketplaceController.java`
- DTO: `exact/path/MarketplaceTemplateDto.java`
- Unit test: `exact/path/MarketplaceConfigServiceTest.java`
- Integration test: `exact/path/MarketplaceIntegrationTest.java`

### 7. Additional Notes
- Any surprises or concerns
```

---

## STOP Condition

If you discover blocking issues (e.g., conflicting patterns, missing dependencies), report them and STOP.
Do NOT attempt to fix issues — that's for APPLY phase.
