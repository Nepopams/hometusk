# Codex APPLY: ST-1302 — ShoppingRun REST Endpoints

## Context
Implement REST endpoints for ShoppingRun lifecycle per OpenAPI contract. Follow existing ShoppingController patterns.

## Files to Create

| File | Purpose |
|------|---------|
| `services/backend/src/main/java/com/hometusk/shopping/dto/CreateShoppingRunRequest.java` | Request DTO |
| `services/backend/src/main/java/com/hometusk/shopping/dto/CloseShoppingRunRequest.java` | Request DTO |
| `services/backend/src/main/java/com/hometusk/shopping/dto/UpdateRunItemRequest.java` | Request DTO |
| `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingRunDto.java` | Full response DTO |
| `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingRunSummaryDto.java` | List response DTO |
| `services/backend/src/main/java/com/hometusk/shopping/dto/ShoppingRunItemDto.java` | Item DTO |
| `services/backend/src/main/java/com/hometusk/shopping/dto/ItemCountsDto.java` | Counts DTO |
| `services/backend/src/main/java/com/hometusk/shopping/service/ShoppingRunService.java` | Business logic |
| `services/backend/src/main/java/com/hometusk/shopping/api/ShoppingRunController.java` | REST controller |
| `services/backend/src/test/java/com/hometusk/integration/shopping/ShoppingRunEndpointIntegrationTest.java` | Integration tests |

---

## Step 1: Create Request DTOs

**CreateShoppingRunRequest.java:**
```java
package com.hometusk.shopping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Request to create a shopping run")
public record CreateShoppingRunRequest(
    @NotNull
    @Schema(description = "Shopping list ID to create run from")
    UUID listId
) {}
```

**CloseShoppingRunRequest.java:**
```java
package com.hometusk.shopping.dto;

import com.hometusk.shopping.ShoppingRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to close a shopping run")
public record CloseShoppingRunRequest(
    @NotNull
    @Schema(description = "Final status (COMPLETED or CANCELLED)")
    ShoppingRunStatus status
) {}
```

**UpdateRunItemRequest.java:**
```java
package com.hometusk.shopping.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update a run item")
public record UpdateRunItemRequest(
    @NotNull
    @Schema(description = "Whether item is purchased")
    Boolean purchased,

    @Schema(description = "Sync purchase status to original list item (default true)")
    Boolean syncToList
) {
    public boolean shouldSyncToList() {
        return syncToList == null || syncToList;
    }
}
```

---

## Step 2: Create Response DTOs

**ItemCountsDto.java:**
```java
package com.hometusk.shopping.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Item count summary")
public record ItemCountsDto(
    @Schema(description = "Total items") int total,
    @Schema(description = "Purchased items") int purchased,
    @Schema(description = "Remaining items") int remaining
) {
    public static ItemCountsDto from(int total, int purchased) {
        return new ItemCountsDto(total, purchased, total - purchased);
    }
}
```

**ShoppingRunItemDto.java:**
```java
package com.hometusk.shopping.dto;

import com.hometusk.shopping.ShoppingRunItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Item snapshot within a shopping run")
public record ShoppingRunItemDto(
    @Schema(description = "Run item ID") UUID id,
    @Schema(description = "Original ShoppingItem ID") UUID originalItemId,
    @Schema(description = "Item name") String name,
    @Schema(description = "Quantity") Integer quantity,
    @Schema(description = "Unit") String unit,
    @Schema(description = "Purchased in this run") boolean purchased,
    @Schema(description = "When purchased") Instant purchasedAt
) {
    public static ShoppingRunItemDto from(ShoppingRunItem item) {
        return new ShoppingRunItemDto(
            item.getId(),
            item.getOriginalItemId(),
            item.getName(),
            item.getQuantity(),
            item.getUnit(),
            item.isPurchased(),
            item.getPurchasedAt()
        );
    }
}
```

**ShoppingRunDto.java:**
```java
package com.hometusk.shopping.dto;

import com.hometusk.shopping.ShoppingRun;
import com.hometusk.user.dto.UserSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Full shopping run with items")
public record ShoppingRunDto(
    @Schema(description = "Run ID") UUID id,
    @Schema(description = "Household ID") UUID householdId,
    @Schema(description = "Source list ID") UUID listId,
    @Schema(description = "List name snapshot") String listName,
    @Schema(description = "Run status") String status,
    @Schema(description = "Created by") UserSummaryDto createdBy,
    @Schema(description = "Created at") Instant createdAt,
    @Schema(description = "Closed at") Instant closedAt,
    @Schema(description = "Items") List<ShoppingRunItemDto> items,
    @Schema(description = "Item counts") ItemCountsDto itemCounts
) {
    public static ShoppingRunDto from(ShoppingRun run) {
        List<ShoppingRunItemDto> items = run.getItems().stream()
            .map(ShoppingRunItemDto::from)
            .toList();
        int purchased = (int) run.getItems().stream().filter(i -> i.isPurchased()).count();
        return new ShoppingRunDto(
            run.getId(),
            run.getHouseholdId(),
            run.getSourceListId(),
            run.getListName(),
            run.getStatus().name(),
            UserSummaryDto.from(run.getCreatedBy()),
            run.getCreatedAt(),
            run.getClosedAt(),
            items,
            ItemCountsDto.from(items.size(), purchased)
        );
    }
}
```

**ShoppingRunSummaryDto.java:**
```java
package com.hometusk.shopping.dto;

import com.hometusk.shopping.ShoppingRun;
import com.hometusk.user.dto.UserSummaryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Shopping run summary (without items)")
public record ShoppingRunSummaryDto(
    UUID id,
    UUID householdId,
    UUID listId,
    String listName,
    String status,
    UserSummaryDto createdBy,
    Instant createdAt,
    Instant closedAt,
    ItemCountsDto itemCounts
) {
    public static ShoppingRunSummaryDto from(ShoppingRun run) {
        int purchased = (int) run.getItems().stream().filter(i -> i.isPurchased()).count();
        return new ShoppingRunSummaryDto(
            run.getId(),
            run.getHouseholdId(),
            run.getSourceListId(),
            run.getListName(),
            run.getStatus().name(),
            UserSummaryDto.from(run.getCreatedBy()),
            run.getCreatedAt(),
            run.getClosedAt(),
            ItemCountsDto.from(run.getItems().size(), purchased)
        );
    }
}
```

---

## Step 3: Create ShoppingRunService

**ShoppingRunService.java:**
```java
package com.hometusk.shopping.service;

import com.hometusk.household.Household;
import com.hometusk.household.HouseholdRepository;
import com.hometusk.shopping.*;
import com.hometusk.user.User;
import com.hometusk.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ShoppingRunService {

    private final ShoppingRunRepository runRepository;
    private final ShoppingListRepository listRepository;
    private final ShoppingItemRepository itemRepository;
    private final HouseholdRepository householdRepository;
    private final UserRepository userRepository;

    // Constructor injection

    @Transactional
    public ShoppingRun createRun(UUID householdId, UUID listId, UUID userId) {
        Household household = householdRepository.findById(householdId)
            .orElseThrow(() -> new IllegalArgumentException("Household not found"));

        ShoppingList list = listRepository.findByIdAndHousehold_Id(listId, householdId)
            .orElseThrow(() -> new IllegalArgumentException("List not found"));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<ShoppingItem> unpurchased = itemRepository.findByShoppingList_IdAndPurchasedFalse(listId);
        if (unpurchased.isEmpty()) {
            throw new IllegalStateException("List has no unpurchased items");
        }

        ShoppingRun run = new ShoppingRun();
        run.setHousehold(household);
        run.setSourceList(list);
        run.setListName(list.getName());
        run.setStatus(ShoppingRunStatus.ACTIVE);
        run.setCreatedBy(user);
        run.setCreatedAt(Instant.now());

        for (ShoppingItem item : unpurchased) {
            run.addItem(ShoppingRunItem.fromShoppingItem(run, item));
        }

        return runRepository.save(run);
    }

    @Transactional(readOnly = true)
    public List<ShoppingRun> listRuns(UUID householdId, ShoppingRunStatus status, int limit) {
        if (status == null) {
            return runRepository.findByHousehold_IdOrderByCreatedAtDesc(householdId)
                .stream().limit(limit).toList();
        }
        return runRepository.findByHousehold_IdAndStatusOrderByCreatedAtDesc(householdId, status)
            .stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public ShoppingRun getRun(UUID householdId, UUID runId) {
        return runRepository.findByIdAndHousehold_Id(runId, householdId)
            .orElseThrow(() -> new IllegalArgumentException("Run not found"));
    }

    @Transactional
    public ShoppingRun closeRun(UUID householdId, UUID runId, ShoppingRunStatus status) {
        if (status != ShoppingRunStatus.COMPLETED && status != ShoppingRunStatus.CANCELLED) {
            throw new IllegalArgumentException("Status must be COMPLETED or CANCELLED");
        }

        ShoppingRun run = getRun(householdId, runId);

        if (run.getStatus() != ShoppingRunStatus.ACTIVE) {
            if (run.getStatus() == status) {
                return run; // Idempotent
            }
            throw new IllegalStateException("Run already closed with different status");
        }

        run.setStatus(status);
        run.setClosedAt(Instant.now());
        return runRepository.save(run);
    }

    @Transactional
    public ShoppingRunItem updateItem(UUID householdId, UUID runId, UUID itemId,
                                       boolean purchased, boolean syncToList) {
        ShoppingRun run = getRun(householdId, runId);

        if (run.getStatus() != ShoppingRunStatus.ACTIVE) {
            throw new IllegalStateException("Cannot update items in closed run");
        }

        ShoppingRunItem item = run.getItems().stream()
            .filter(i -> i.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Item not found in run"));

        item.setPurchased(purchased);
        item.setPurchasedAt(purchased ? Instant.now() : null);

        if (syncToList && purchased && item.getOriginalItemId() != null) {
            itemRepository.findById(item.getOriginalItemId())
                .ifPresent(original -> {
                    original.setPurchased(true);
                    original.setPurchasedAt(Instant.now());
                    itemRepository.save(original);
                });
        }

        runRepository.save(run);
        return item;
    }
}
```

---

## Step 4: Create ShoppingRunController

**ShoppingRunController.java:**
```java
package com.hometusk.shopping.api;

import com.hometusk.auth.CurrentUser;
import com.hometusk.auth.UserResolver;
import com.hometusk.household.MembershipService;
import com.hometusk.shopping.*;
import com.hometusk.shopping.dto.*;
import com.hometusk.shopping.service.ShoppingRunService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/households/{householdId}/shopping-runs")
@Tag(name = "ShoppingRuns", description = "Shopping run management")
public class ShoppingRunController {

    private final ShoppingRunService runService;
    private final MembershipService membershipService;
    private final UserResolver userResolver;

    // Constructor injection

    @PostMapping
    @Operation(summary = "Create a shopping run from a list")
    public ResponseEntity<ShoppingRunDto> createRun(
            @PathVariable UUID householdId,
            @Valid @RequestBody CreateShoppingRunRequest request) {
        CurrentUser user = userResolver.resolveCurrentUser();
        membershipService.requireMembership(user.id(), householdId);

        ShoppingRun run = runService.createRun(householdId, request.listId(), user.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ShoppingRunDto.from(run));
    }

    @GetMapping
    @Operation(summary = "List shopping runs")
    public ResponseEntity<List<ShoppingRunSummaryDto>> listRuns(
            @PathVariable UUID householdId,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status,
            @RequestParam(required = false, defaultValue = "20") int limit) {
        CurrentUser user = userResolver.resolveCurrentUser();
        membershipService.requireMembership(user.id(), householdId);

        ShoppingRunStatus statusEnum = "all".equalsIgnoreCase(status) ? null
            : ShoppingRunStatus.valueOf(status);

        List<ShoppingRun> runs = runService.listRuns(householdId, statusEnum, Math.min(limit, 100));
        return ResponseEntity.ok(runs.stream().map(ShoppingRunSummaryDto::from).toList());
    }

    @GetMapping("/{runId}")
    @Operation(summary = "Get shopping run details")
    public ResponseEntity<ShoppingRunDto> getRun(
            @PathVariable UUID householdId,
            @PathVariable UUID runId) {
        CurrentUser user = userResolver.resolveCurrentUser();
        membershipService.requireMembership(user.id(), householdId);

        ShoppingRun run = runService.getRun(householdId, runId);
        return ResponseEntity.ok(ShoppingRunDto.from(run));
    }

    @PostMapping("/{runId}/close")
    @Operation(summary = "Close a shopping run")
    public ResponseEntity<ShoppingRunDto> closeRun(
            @PathVariable UUID householdId,
            @PathVariable UUID runId,
            @Valid @RequestBody CloseShoppingRunRequest request) {
        CurrentUser user = userResolver.resolveCurrentUser();
        membershipService.requireMembership(user.id(), householdId);

        ShoppingRun run = runService.closeRun(householdId, runId, request.status());
        return ResponseEntity.ok(ShoppingRunDto.from(run));
    }

    @PatchMapping("/{runId}/items/{itemId}")
    @Operation(summary = "Update item status in a run")
    public ResponseEntity<ShoppingRunItemDto> updateItem(
            @PathVariable UUID householdId,
            @PathVariable UUID runId,
            @PathVariable UUID itemId,
            @Valid @RequestBody UpdateRunItemRequest request) {
        CurrentUser user = userResolver.resolveCurrentUser();
        membershipService.requireMembership(user.id(), householdId);

        ShoppingRunItem item = runService.updateItem(
            householdId, runId, itemId,
            request.purchased(),
            request.shouldSyncToList()
        );
        return ResponseEntity.ok(ShoppingRunItemDto.from(item));
    }
}
```

---

## Step 5: Create Integration Tests

**ShoppingRunEndpointIntegrationTest.java:**

Test cases to implement:
1. `createRun_success` — creates run with snapshotted items
2. `createRun_emptyList_returns400` — no unpurchased items
3. `listRuns_filterByStatus` — returns only matching status
4. `getRun_returnsAllItems` — full details with items
5. `closeRun_completed` — sets status and closedAt
6. `closeRun_alreadyClosed_sameStatus_idempotent`
7. `closeRun_alreadyClosed_differentStatus_returns409`
8. `updateItem_purchased_syncsToList`
9. `updateItem_closedRun_returns400`
10. `householdBoundary_returns403or404`

Follow pattern from `ShoppingIntegrationTest`:
- Extend `AiPlatformIntegrationTestBase`
- Use `mockMvc.perform(...)` with `jwt()` auth
- Setup test data in `@BeforeEach`

---

## Verification

```bash
cd /home/vad/Документы/hometusk/services/backend

# Compile
./gradlew compileJava

# Run specific tests
./gradlew test --tests "*ShoppingRunEndpoint*"

# All tests
./gradlew test
```

---

## Constraints
- Follow existing DTO record pattern with `static from()` factory
- Use `membershipService.requireMembership()` for household boundary
- Use `@Transactional` appropriately (readOnly for queries)
- Check for `UserSummaryDto` — may need to create if not exists
- Handle exceptions appropriately (return 400/404/409 per contract)
