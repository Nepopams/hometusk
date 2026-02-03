# Codex PLAN Prompt: ST-1301 — ShoppingRun Entity + Repository

## Mode
**READ-ONLY EXPLORATION** — Do NOT edit, write, or modify any files.

## Objective
Explore the codebase to gather information needed to implement ST-1301 (ShoppingRun Entity + Repository). Report findings for the APPLY phase.

---

## Sources of Truth (READ these files)

```bash
cat docs/planning/workpacks/ST-1301/workpack.md
cat docs/adr/014-shopping-run-entity-design.md
cat docs/contracts/http/shopping-marketplaces.openapi.yaml
```

---

## Exploration Tasks

### 1. Check existing migrations
Find the latest migration version to confirm V025 is available.

```bash
ls -la services/backend/src/main/resources/db/migration/
```

**Report:**
- Latest migration file name
- Confirm V025 is not taken

### 2. Examine ShoppingList entity (pattern reference)
Understand existing entity patterns, annotations, relationships.

```bash
cat services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingList.java
```

**Report:**
- Entity annotations used (@Entity, @Table, @Column, etc.)
- Relationship annotations (@ManyToOne, @OneToMany fetch/cascade settings)
- Constructor patterns
- Getter patterns (Lombok or manual)
- Audit fields (createdAt, etc.)

### 3. Examine ShoppingItem entity (pattern reference)
Understand item entity structure for snapshot design.

```bash
cat services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingItem.java
```

**Report:**
- Fields: name, quantity, unit, purchased, purchasedAt
- markPurchased() / unmarkPurchased() method signatures
- Factory methods if any

### 4. Examine Household entity
Understand Household reference pattern.

```bash
cat services/backend/src/main/java/com/hometusk/household/domain/Household.java
```

**Report:**
- How other entities reference Household
- UUID field naming (householdId vs household.id)

### 5. Examine User entity
Understand User reference pattern.

```bash
cat services/backend/src/main/java/com/hometusk/user/domain/User.java
```

**Report:**
- How User is referenced in entities
- Package path

### 6. Examine ShoppingListRepository (pattern reference)
Understand repository method conventions and household-scoped queries.

```bash
cat services/backend/src/main/java/com/hometusk/shopping/repository/ShoppingListRepository.java
```

**Report:**
- Interface structure (extends JpaRepository?)
- Household-scoped query method names
- Any @Query annotations used

### 7. Examine ShoppingItemRepository (pattern reference)

```bash
cat services/backend/src/main/java/com/hometusk/shopping/repository/ShoppingItemRepository.java
```

**Report:**
- Method naming conventions
- Derived query patterns

### 8. Find existing unit test examples

```bash
ls services/backend/src/test/java/com/hometusk/shopping/domain/
cat services/backend/src/test/java/com/hometusk/shopping/domain/ShoppingListTest.java 2>/dev/null || echo "File not found"
cat services/backend/src/test/java/com/hometusk/shopping/domain/ShoppingItemTest.java 2>/dev/null || echo "File not found"
```

**Report:**
- Test class structure
- Test method naming conventions
- Mocking patterns used
- Assertions library (JUnit5, AssertJ?)

### 9. Find existing integration test examples

```bash
ls services/backend/src/test/java/com/hometusk/integration/
ls services/backend/src/test/java/com/hometusk/integration/shopping/ 2>/dev/null || echo "Directory not found"
```

```bash
find services/backend/src/test/java -name "*RepositoryIntegrationTest.java" | head -3
```

Then examine one for patterns:
```bash
cat services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingListRepositoryIntegrationTest.java 2>/dev/null || cat "$(find services/backend/src/test/java -name '*RepositoryIntegrationTest.java' | head -1)"
```

**Report:**
- Test class annotations (@SpringBootTest, @DataJpaTest, @Testcontainers?)
- Test data setup patterns
- Household/User fixture creation
- Assertion patterns

### 10. Check existing enums in shopping domain

```bash
ls services/backend/src/main/java/com/hometusk/shopping/domain/
```

**Report:**
- Any existing enums for pattern reference
- Package structure confirmation

### 11. Check contract DTOs for ShoppingRun

```bash
rg -A 20 "ShoppingRunDto:" docs/contracts/http/shopping-marketplaces.openapi.yaml
rg -A 15 "ShoppingRunItemDto:" docs/contracts/http/shopping-marketplaces.openapi.yaml
rg -A 5 "ItemCounts:" docs/contracts/http/shopping-marketplaces.openapi.yaml
```

**Report:**
- Required fields in DTOs (must match entity)
- ItemCounts structure

---

## Forbidden Commands

DO NOT execute:
- Any file modifications (edit, write, mv, rm)
- `./gradlew build`, `./gradlew test`, or any build commands
- `git commit`, `git push`, or any git write operations
- Any network requests or database operations

---

## Output Format

Provide a structured report with the following sections:

```
## PLAN Findings: ST-1301

### 1. Migration Status
- Latest migration: V0xx__name.sql
- V025 available: YES/NO

### 2. Entity Patterns (from ShoppingList/ShoppingItem)
- Lombok: YES/NO
- UUID generation: @GeneratedValue(strategy = ...)
- Relationship fetch: LAZY/EAGER
- Cascade settings: ...
- Audit fields: createdAt pattern

### 3. Repository Patterns
- Base interface: JpaRepository<T, UUID>
- Household-scoped naming: findByIdAndHousehold_Id / findByIdAndHouseholdId
- Query derivation vs @Query

### 4. Test Patterns
- Unit test framework: JUnit5 + ...
- Integration test annotations: ...
- Fixture creation: ...

### 5. Package Structure Confirmation
- Entity path: services/backend/src/main/java/com/hometusk/shopping/domain/
- Repository path: services/backend/src/main/java/com/hometusk/shopping/repository/
- Unit test path: services/backend/src/test/java/com/hometusk/shopping/domain/
- Integration test path: services/backend/src/test/java/com/hometusk/integration/shopping/

### 6. Contract-Entity Alignment
- ShoppingRunDto fields: id, sourceListId, listName, status, createdAt, closedAt, itemCounts
- ShoppingRunItemDto fields: id, name, quantity, unit, purchased, purchasedAt

### 7. Blockers / Questions
- (List any findings that might affect implementation)
```

---

## STOP-THE-LINE

If you encounter:
- Missing files or unexpected structure → report in "Blockers"
- Conflicting patterns → document both and note which to follow
- V025 already exists → STOP and report conflict

Do NOT proceed to implementation. Report findings only.
