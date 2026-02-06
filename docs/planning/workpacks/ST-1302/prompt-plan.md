# Codex PLAN: ST-1302 — ShoppingRun REST Endpoints

## Objective
Explore existing code to understand patterns for implementing ShoppingRun endpoints.

## Constraints
- **READ-ONLY** — no file modifications
- Allowed commands: `ls`, `cat`, `rg`, `grep`, `find`

## Questions to Answer

### Q1: ShoppingRun entity structure
- Read `services/backend/src/main/java/com/hometusk/shopping/ShoppingRun.java`
- Read `services/backend/src/main/java/com/hometusk/shopping/ShoppingRunItem.java`
- What fields exist?
- What is the relationship structure?

### Q2: ShoppingRunRepository methods
- Read `services/backend/src/main/java/com/hometusk/shopping/ShoppingRunRepository.java`
- What query methods exist?
- Are there methods for filtering by status?

### Q3: Existing controller patterns
- Read `services/backend/src/main/java/com/hometusk/shopping/api/ShoppingController.java`
- How is household membership checked?
- How are DTOs structured?
- What annotations are used?

### Q4: Existing service patterns
- Read `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingService.java` (if exists)
- Or another service for patterns
- How is idempotency handled?

### Q5: DTO patterns
- Run `ls services/backend/src/main/java/com/hometusk/shopping/dto/`
- What DTOs already exist?
- Are they records or classes?

### Q6: Test patterns
- Run `ls services/backend/src/test/java/com/hometusk/integration/shopping/`
- What test files exist?
- What test utilities are used?

### Q7: ShoppingList and ShoppingItem access
- How to get items from a list?
- How to check if item belongs to household?

## Expected Output

```
## Findings

### ShoppingRun Entity
- Fields: [list]
- Status enum: [values]
- Relationships: [describe]

### ShoppingRunItem Entity
- Fields: [list]
- Original item reference: [how]

### Repository
- Query methods: [list]
- Status filter: [yes/no, how]

### Controller Pattern
- Membership check: [method]
- Annotations: [list]
- DTO mapping: [approach]

### Service Pattern
- Idempotency: [approach]
- Transaction handling: [notes]

### Existing DTOs
- Files: [list]
- Style: [record/class]

### Test Pattern
- Base class: [if any]
- Utilities: [list]
- Testcontainers: [yes/no]

### Recommended Implementation
[Notes on approach based on findings]
```
