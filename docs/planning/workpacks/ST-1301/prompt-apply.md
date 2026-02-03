# Codex APPLY Prompt: ST-1301 — ShoppingRun Entity + Repository

## Mode
**IMPLEMENTATION** — Create files, write code, run tests.

## Objective
Implement ShoppingRun and ShoppingRunItem JPA entities with repositories and DB migration V025.

---

## Sources of Truth

- Workpack: `docs/planning/workpacks/ST-1301/workpack.md`
- ADR: `docs/adr/014-shopping-run-entity-design.md`
- Contract: `docs/contracts/http/shopping-marketplaces.openapi.yaml`
- Checklist: `docs/planning/workpacks/ST-1301/checklist.md`

---

## PLAN Findings (use these patterns)

### Entity Patterns
- **NO Lombok** — use manual getters, explicit constructors
- UUID: `@GeneratedValue(strategy = GenerationType.UUID)`
- Relationships: `@ManyToOne(fetch = FetchType.LAZY)`
- OneToMany: `cascade = CascadeType.ALL, orphanRemoval = true`
- Protected no-args constructor + public constructor setting defaults
- `createdAt` set in constructor as `Instant.now()`
- Use `@Transient` helpers like `getHouseholdId()` for convenience

### Repository Patterns
- Extend `JpaRepository<T, UUID>`
- Household-scoped: use `findByIdAndHousehold_Id` (underscore notation)
- Derived queries preferred; `@Query` only when joins needed

### Test Patterns
- Unit tests: JUnit5 + AssertJ, **create folder** `shopping/domain/`
- Integration tests: extend `IntegrationTestBase`
- Use `@Transactional`, fixtures from base class

### Migration
- Next available: **V025**
- Use `IF NOT EXISTS` guards for idempotency

### Contract Note
- Contract shows `originalItemId` as required, but it can be null after deletion
- In DTO mapping: return `null` for deleted items (API consumers handle this)

---

## Files to Create (8 files)

### 1. DB Migration
**Path:** `services/backend/src/main/resources/db/migration/V025__create_shopping_runs.sql`

```sql
-- Shopping runs (shopping trip snapshots)
CREATE TABLE IF NOT EXISTS shopping_runs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    household_id    UUID NOT NULL,
    source_list_id  UUID NOT NULL,
    list_name       VARCHAR(255) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by_id   UUID NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    closed_at       TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_shopping_runs_household
        FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE CASCADE,
    CONSTRAINT fk_shopping_runs_source_list
        FOREIGN KEY (source_list_id) REFERENCES shopping_lists(id) ON DELETE RESTRICT,
    CONSTRAINT fk_shopping_runs_created_by
        FOREIGN KEY (created_by_id) REFERENCES users(id),
    CONSTRAINT chk_shopping_runs_status
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED'))
);

-- Shopping run items (snapshots of list items)
CREATE TABLE IF NOT EXISTS shopping_run_items (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id            UUID NOT NULL,
    original_item_id  UUID,
    name              VARCHAR(255) NOT NULL,
    quantity          INTEGER NOT NULL DEFAULT 1,
    unit              VARCHAR(50),
    purchased         BOOLEAN NOT NULL DEFAULT FALSE,
    purchased_at      TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_shopping_run_items_run
        FOREIGN KEY (run_id) REFERENCES shopping_runs(id) ON DELETE CASCADE,
    CONSTRAINT fk_shopping_run_items_original
        FOREIGN KEY (original_item_id) REFERENCES shopping_items(id) ON DELETE SET NULL
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_shopping_runs_household_id
    ON shopping_runs(household_id);
CREATE INDEX IF NOT EXISTS idx_shopping_runs_source_list_id
    ON shopping_runs(source_list_id);
CREATE INDEX IF NOT EXISTS idx_shopping_run_items_run_id
    ON shopping_run_items(run_id);
CREATE INDEX IF NOT EXISTS idx_shopping_run_items_original_item_id
    ON shopping_run_items(original_item_id)
    WHERE original_item_id IS NOT NULL;
```

### 2. ShoppingRunStatus Enum
**Path:** `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingRunStatus.java`

```java
package com.hometusk.shopping.domain;

public enum ShoppingRunStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}
```

### 3. ItemCounts Record
**Path:** `services/backend/src/main/java/com/hometusk/shopping/domain/ItemCounts.java`

```java
package com.hometusk.shopping.domain;

public record ItemCounts(int total, int purchased, int remaining) {
    public static ItemCounts of(int total, int purchased) {
        return new ItemCounts(total, purchased, total - purchased);
    }
}
```

### 4. ShoppingRunItem Entity
**Path:** `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingRunItem.java`

```java
package com.hometusk.shopping.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shopping_run_items")
public class ShoppingRunItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id", nullable = false)
    private ShoppingRun run;

    @Column(name = "original_item_id")
    private UUID originalItemId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer quantity;

    @Column
    private String unit;

    @Column(nullable = false)
    private boolean purchased;

    @Column(name = "purchased_at")
    private Instant purchasedAt;

    protected ShoppingRunItem() {
        // JPA
    }

    private ShoppingRunItem(ShoppingRun run, UUID originalItemId, String name, Integer quantity, String unit) {
        this.run = run;
        this.originalItemId = originalItemId;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.purchased = false;
        this.purchasedAt = null;
    }

    public static ShoppingRunItem fromShoppingItem(ShoppingRun run, ShoppingItem item) {
        return new ShoppingRunItem(
            run,
            item.getId(),
            item.getName(),
            item.getQuantity(),
            item.getUnit()
        );
    }

    public void markPurchased() {
        if (!this.purchased) {
            this.purchased = true;
            this.purchasedAt = Instant.now();
        }
    }

    public void unmarkPurchased() {
        this.purchased = false;
        this.purchasedAt = null;
    }

    // Getters
    public UUID getId() { return id; }
    public ShoppingRun getRun() { return run; }
    public UUID getOriginalItemId() { return originalItemId; }
    public String getName() { return name; }
    public Integer getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public boolean isPurchased() { return purchased; }
    public Instant getPurchasedAt() { return purchasedAt; }
}
```

### 5. ShoppingRun Entity
**Path:** `services/backend/src/main/java/com/hometusk/shopping/domain/ShoppingRun.java`

```java
package com.hometusk.shopping.domain;

import com.hometusk.household.domain.Household;
import com.hometusk.user.domain.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shopping_runs")
public class ShoppingRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_list_id", nullable = false)
    private ShoppingList sourceList;

    @Column(name = "list_name", nullable = false)
    private String listName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShoppingRunStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShoppingRunItem> items = new ArrayList<>();

    protected ShoppingRun() {
        // JPA
    }

    public ShoppingRun(Household household, ShoppingList sourceList, User createdBy) {
        this.household = household;
        this.sourceList = sourceList;
        this.listName = sourceList.getName();
        this.status = ShoppingRunStatus.ACTIVE;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.closedAt = null;
    }

    public void close(ShoppingRunStatus newStatus) {
        if (newStatus == ShoppingRunStatus.ACTIVE) {
            throw new IllegalArgumentException("Cannot close run with ACTIVE status");
        }
        if (this.status != ShoppingRunStatus.ACTIVE) {
            if (this.status == newStatus) {
                // Idempotent: already closed with same status
                return;
            }
            throw new IllegalStateException(
                "Cannot change status from " + this.status + " to " + newStatus
            );
        }
        this.status = newStatus;
        this.closedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == ShoppingRunStatus.ACTIVE;
    }

    public void addItem(ShoppingRunItem item) {
        this.items.add(item);
    }

    public ItemCounts getItemCounts() {
        int total = items.size();
        int purchased = (int) items.stream().filter(ShoppingRunItem::isPurchased).count();
        return ItemCounts.of(total, purchased);
    }

    // Getters
    public UUID getId() { return id; }
    public Household getHousehold() { return household; }
    public ShoppingList getSourceList() { return sourceList; }
    public String getListName() { return listName; }
    public ShoppingRunStatus getStatus() { return status; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClosedAt() { return closedAt; }
    public List<ShoppingRunItem> getItems() { return items; }

    @Transient
    public UUID getHouseholdId() {
        return household != null ? household.getId() : null;
    }

    @Transient
    public UUID getSourceListId() {
        return sourceList != null ? sourceList.getId() : null;
    }

    @Transient
    public UUID getCreatedById() {
        return createdBy != null ? createdBy.getId() : null;
    }
}
```

### 6. ShoppingRunRepository
**Path:** `services/backend/src/main/java/com/hometusk/shopping/repository/ShoppingRunRepository.java`

```java
package com.hometusk.shopping.repository;

import com.hometusk.shopping.domain.ShoppingRun;
import com.hometusk.shopping.domain.ShoppingRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShoppingRunRepository extends JpaRepository<ShoppingRun, UUID> {

    Optional<ShoppingRun> findByIdAndHousehold_Id(UUID id, UUID householdId);

    List<ShoppingRun> findByHousehold_IdOrderByCreatedAtDesc(UUID householdId);

    List<ShoppingRun> findByHousehold_IdAndStatusOrderByCreatedAtDesc(UUID householdId, ShoppingRunStatus status);

    boolean existsByIdAndHousehold_Id(UUID id, UUID householdId);
}
```

### 7. ShoppingRunItemRepository
**Path:** `services/backend/src/main/java/com/hometusk/shopping/repository/ShoppingRunItemRepository.java`

```java
package com.hometusk.shopping.repository;

import com.hometusk.shopping.domain.ShoppingRunItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShoppingRunItemRepository extends JpaRepository<ShoppingRunItem, UUID> {

    List<ShoppingRunItem> findByRun_Id(UUID runId);

    long countByRun_IdAndPurchasedTrue(UUID runId);
}
```

### 8. Unit Tests
**Path:** `services/backend/src/test/java/com/hometusk/shopping/domain/ShoppingRunTest.java`

```java
package com.hometusk.shopping.domain;

import com.hometusk.household.domain.Household;
import com.hometusk.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;

class ShoppingRunTest {

    private Household household;
    private ShoppingList shoppingList;
    private User user;

    @BeforeEach
    void setUp() {
        household = new Household("Test Household");
        shoppingList = new ShoppingList(household, "Grocery List");
        user = new User("test@example.com", "Test User");
    }

    @Test
    void newRun_hasActiveStatus_andCreatedAt() {
        Instant before = Instant.now();
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        Instant after = Instant.now();

        assertThat(run.getStatus()).isEqualTo(ShoppingRunStatus.ACTIVE);
        assertThat(run.isActive()).isTrue();
        assertThat(run.getCreatedAt()).isBetween(before, after);
        assertThat(run.getClosedAt()).isNull();
        assertThat(run.getListName()).isEqualTo("Grocery List");
    }

    @Test
    void snapshotItem_copiesDataFromShoppingItem() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        ShoppingItem item = shoppingList.addItem("Milk", 2, "liters");

        ShoppingRunItem runItem = ShoppingRunItem.fromShoppingItem(run, item);

        assertThat(runItem.getName()).isEqualTo("Milk");
        assertThat(runItem.getQuantity()).isEqualTo(2);
        assertThat(runItem.getUnit()).isEqualTo("liters");
        assertThat(runItem.getOriginalItemId()).isEqualTo(item.getId());
        assertThat(runItem.isPurchased()).isFalse();
        assertThat(runItem.getPurchasedAt()).isNull();
    }

    @Test
    void closeAsCompleted_setsStatusAndClosedAt() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        Instant before = Instant.now();

        run.close(ShoppingRunStatus.COMPLETED);

        assertThat(run.getStatus()).isEqualTo(ShoppingRunStatus.COMPLETED);
        assertThat(run.isActive()).isFalse();
        assertThat(run.getClosedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void closeAsCancelled_setsStatusAndClosedAt() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);

        run.close(ShoppingRunStatus.CANCELLED);

        assertThat(run.getStatus()).isEqualTo(ShoppingRunStatus.CANCELLED);
        assertThat(run.isActive()).isFalse();
        assertThat(run.getClosedAt()).isNotNull();
    }

    @Test
    void closeAlreadyClosed_sameStatus_isIdempotent() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        run.close(ShoppingRunStatus.COMPLETED);
        Instant firstClosedAt = run.getClosedAt();

        // Should not throw, idempotent
        run.close(ShoppingRunStatus.COMPLETED);

        assertThat(run.getStatus()).isEqualTo(ShoppingRunStatus.COMPLETED);
        assertThat(run.getClosedAt()).isEqualTo(firstClosedAt);
    }

    @Test
    void closeAlreadyClosed_differentStatus_throwsException() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        run.close(ShoppingRunStatus.COMPLETED);

        assertThatThrownBy(() -> run.close(ShoppingRunStatus.CANCELLED))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot change status");
    }

    @Test
    void closeWithActiveStatus_throwsException() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);

        assertThatThrownBy(() -> run.close(ShoppingRunStatus.ACTIVE))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot close run with ACTIVE status");
    }

    @Test
    void markItemPurchased_setsTimestamp() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        ShoppingItem item = shoppingList.addItem("Bread", 1, null);
        ShoppingRunItem runItem = ShoppingRunItem.fromShoppingItem(run, item);

        Instant before = Instant.now();
        runItem.markPurchased();

        assertThat(runItem.isPurchased()).isTrue();
        assertThat(runItem.getPurchasedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void markItemPurchased_isIdempotent() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        ShoppingItem item = shoppingList.addItem("Bread", 1, null);
        ShoppingRunItem runItem = ShoppingRunItem.fromShoppingItem(run, item);

        runItem.markPurchased();
        Instant firstPurchasedAt = runItem.getPurchasedAt();

        runItem.markPurchased();

        assertThat(runItem.getPurchasedAt()).isEqualTo(firstPurchasedAt);
    }

    @Test
    void unmarkItemPurchased_clearsTimestamp() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        ShoppingItem item = shoppingList.addItem("Bread", 1, null);
        ShoppingRunItem runItem = ShoppingRunItem.fromShoppingItem(run, item);

        runItem.markPurchased();
        runItem.unmarkPurchased();

        assertThat(runItem.isPurchased()).isFalse();
        assertThat(runItem.getPurchasedAt()).isNull();
    }

    @Test
    void getItemCounts_returnsCorrectValues() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);

        ShoppingItem item1 = shoppingList.addItem("Milk", 1, null);
        ShoppingItem item2 = shoppingList.addItem("Bread", 1, null);
        ShoppingItem item3 = shoppingList.addItem("Eggs", 1, null);

        ShoppingRunItem runItem1 = ShoppingRunItem.fromShoppingItem(run, item1);
        ShoppingRunItem runItem2 = ShoppingRunItem.fromShoppingItem(run, item2);
        ShoppingRunItem runItem3 = ShoppingRunItem.fromShoppingItem(run, item3);

        run.addItem(runItem1);
        run.addItem(runItem2);
        run.addItem(runItem3);

        runItem1.markPurchased();

        ItemCounts counts = run.getItemCounts();

        assertThat(counts.total()).isEqualTo(3);
        assertThat(counts.purchased()).isEqualTo(1);
        assertThat(counts.remaining()).isEqualTo(2);
    }

    @Test
    void getItemCounts_emptyRun_returnsZeros() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);

        ItemCounts counts = run.getItemCounts();

        assertThat(counts.total()).isEqualTo(0);
        assertThat(counts.purchased()).isEqualTo(0);
        assertThat(counts.remaining()).isEqualTo(0);
    }
}
```

### 9. Integration Tests
**Path:** `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingRunRepositoryIntegrationTest.java`

```java
package com.hometusk.integration.shopping;

import com.hometusk.integration.IntegrationTestBase;
import com.hometusk.shopping.domain.*;
import com.hometusk.shopping.repository.ShoppingListRepository;
import com.hometusk.shopping.repository.ShoppingRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class ShoppingRunRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ShoppingRunRepository shoppingRunRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    private ShoppingList shoppingList;

    @BeforeEach
    void setUpList() {
        shoppingList = new ShoppingList(household, "Test Shopping List");
        shoppingListRepository.save(shoppingList);
    }

    @Test
    void createAndFindRun_works() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        shoppingRunRepository.save(run);

        Optional<ShoppingRun> found = shoppingRunRepository.findByIdAndHousehold_Id(
            run.getId(), household.getId()
        );

        assertThat(found).isPresent();
        assertThat(found.get().getListName()).isEqualTo("Test Shopping List");
        assertThat(found.get().getStatus()).isEqualTo(ShoppingRunStatus.ACTIVE);
    }

    @Test
    void findByHouseholdId_returnsOnlyHouseholdRuns() {
        ShoppingRun run1 = new ShoppingRun(household, shoppingList, user);
        ShoppingRun run2 = new ShoppingRun(household, shoppingList, user);
        shoppingRunRepository.saveAll(List.of(run1, run2));

        List<ShoppingRun> runs = shoppingRunRepository.findByHousehold_IdOrderByCreatedAtDesc(
            household.getId()
        );

        assertThat(runs).hasSize(2);
    }

    @Test
    void findByWrongHousehold_returnsEmpty() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        shoppingRunRepository.save(run);

        // Use otherHousehold from IntegrationTestBase
        Optional<ShoppingRun> found = shoppingRunRepository.findByIdAndHousehold_Id(
            run.getId(), otherHousehold.getId()
        );

        assertThat(found).isEmpty();
    }

    @Test
    void runWithItems_cascadesProperly() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);

        ShoppingItem item1 = shoppingList.addItem("Milk", 2, "liters");
        ShoppingItem item2 = shoppingList.addItem("Bread", 1, null);
        shoppingListRepository.save(shoppingList);

        ShoppingRunItem runItem1 = ShoppingRunItem.fromShoppingItem(run, item1);
        ShoppingRunItem runItem2 = ShoppingRunItem.fromShoppingItem(run, item2);
        run.addItem(runItem1);
        run.addItem(runItem2);

        shoppingRunRepository.save(run);

        ShoppingRun found = shoppingRunRepository.findByIdAndHousehold_Id(
            run.getId(), household.getId()
        ).orElseThrow();

        assertThat(found.getItems()).hasSize(2);
        assertThat(found.getItemCounts().total()).isEqualTo(2);
        assertThat(found.getItemCounts().purchased()).isEqualTo(0);
    }

    @Test
    void findByStatus_filtersCorrectly() {
        ShoppingRun activeRun = new ShoppingRun(household, shoppingList, user);
        ShoppingRun completedRun = new ShoppingRun(household, shoppingList, user);
        completedRun.close(ShoppingRunStatus.COMPLETED);

        shoppingRunRepository.saveAll(List.of(activeRun, completedRun));

        List<ShoppingRun> activeRuns = shoppingRunRepository
            .findByHousehold_IdAndStatusOrderByCreatedAtDesc(
                household.getId(), ShoppingRunStatus.ACTIVE
            );

        assertThat(activeRuns).hasSize(1);
        assertThat(activeRuns.get(0).getId()).isEqualTo(activeRun.getId());
    }

    @Test
    void existsByIdAndHouseholdId_works() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        shoppingRunRepository.save(run);

        assertThat(shoppingRunRepository.existsByIdAndHousehold_Id(
            run.getId(), household.getId()
        )).isTrue();

        assertThat(shoppingRunRepository.existsByIdAndHousehold_Id(
            run.getId(), otherHousehold.getId()
        )).isFalse();
    }
}
```

---

## Verification Commands

After implementation, run:

```bash
cd services/backend

# Format code
./gradlew spotlessApply

# Build
./gradlew build

# Unit tests
./gradlew test --tests "*ShoppingRunTest*"

# Integration tests
./gradlew test --tests "*ShoppingRunRepositoryIntegrationTest*"

# All tests
./gradlew test
```

---

## Acceptance Criteria Checklist

- [ ] AC-1: Entity created with status=ACTIVE, createdAt=now → Unit test
- [ ] AC-2: Item snapshot copies name/qty/unit from ShoppingItem → Unit test
- [ ] AC-3: close() sets status + closedAt → Unit test
- [ ] AC-4: Household boundary in all repository queries → Integration test
- [ ] AC-5: Migration uses IF NOT EXISTS → Migration file

---

## STOP-THE-LINE

If you encounter:
- Compilation errors in existing code → STOP, report
- Test failures in existing tests → STOP, report
- Migration conflicts (V025 exists) → STOP, report
- Missing base classes (IntegrationTestBase, etc.) → adapt or STOP

Do NOT skip tests or ignore failures. All tests must pass.
