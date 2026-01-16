package com.hometusk.shopping.api;

import com.hometusk.activity.service.ActivityRecorder;
import com.hometusk.households.service.HouseholdService;
import com.hometusk.shared.logging.MdcKeys;
import com.hometusk.shared.security.CurrentUser;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.dto.AddShoppingItemRequest;
import com.hometusk.shopping.dto.ShoppingItemDto;
import com.hometusk.shopping.dto.ShoppingListDto;
import com.hometusk.shopping.dto.UpdateShoppingItemRequest;
import com.hometusk.shopping.service.ShoppingService;
import com.hometusk.users.domain.User;
import com.hometusk.users.service.MembershipService;
import com.hometusk.users.service.UserResolver;
import com.hometusk.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/households/{householdId}")
@Tag(name = "Shopping", description = "Shopping list management endpoints")
public class ShoppingController {

    private static final Logger log = LoggerFactory.getLogger(ShoppingController.class);

    private final ShoppingService shoppingService;
    private final MembershipService membershipService;
    private final UserResolver userResolver;
    private final UserService userService;
    private final HouseholdService householdService;
    private final ActivityRecorder activityRecorder;

    public ShoppingController(
            ShoppingService shoppingService,
            MembershipService membershipService,
            UserResolver userResolver,
            UserService userService,
            HouseholdService householdService,
            ActivityRecorder activityRecorder) {
        this.shoppingService = shoppingService;
        this.membershipService = membershipService;
        this.userResolver = userResolver;
        this.userService = userService;
        this.householdService = householdService;
        this.activityRecorder = activityRecorder;
    }

    @GetMapping("/shopping-lists")
    @Operation(
            summary = "List shopping lists",
            description = "Returns all shopping lists in the household with unpurchased item counts.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of shopping lists"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household")
    })
    public ResponseEntity<List<ShoppingListDto>> listShoppingLists(@PathVariable UUID householdId) {
        log.debug("Listing shopping lists for household: {}", householdId);

        // Verify membership (IDOR prevention)
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        // Get all lists with counts
        List<ShoppingList> lists = shoppingService.getListsInHousehold(householdId);
        List<ShoppingListDto> dtos = lists.stream()
                .map(list -> ShoppingListDto.from(list, shoppingService.countUnpurchasedItems(list.getId())))
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/shopping-lists/{listId}/items")
    @Operation(
            summary = "List items in a shopping list",
            description = "Returns items in the shopping list, optionally filtered by purchase status.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of items"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household"),
        @ApiResponse(responseCode = "404", description = "Shopping list not found")
    })
    public ResponseEntity<List<ShoppingItemDto>> listItems(
            @PathVariable UUID householdId,
            @PathVariable UUID listId,
            @RequestParam(required = false) @Parameter(description = "Filter by purchase status") Boolean purchased) {
        log.debug("Listing items for list: {}, household: {}, purchased: {}", listId, householdId, purchased);

        // Verify membership (IDOR prevention)
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        // Get items (service validates list belongs to household)
        List<ShoppingItem> items;
        if (Boolean.FALSE.equals(purchased)) {
            items = shoppingService.getUnpurchasedItemsInList(listId, householdId);
        } else {
            items = shoppingService.getItemsInList(listId, householdId);
        }

        List<ShoppingItemDto> dtos = items.stream().map(ShoppingItemDto::from).toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/shopping-lists/{listId}/items")
    @Operation(
            summary = "Add a shopping item",
            description =
                    """
            Adds a new item to the shopping list. This is a direct REST endpoint
            for simple form submissions (not via commands API).
            See ADR-009 for the Commands vs CRUD boundary decision.
            """)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Item created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household"),
        @ApiResponse(responseCode = "404", description = "Shopping list not found")
    })
    public ResponseEntity<ShoppingItemDto> addItem(
            @PathVariable UUID householdId,
            @PathVariable UUID listId,
            @RequestBody @Valid AddShoppingItemRequest request) {
        log.info("Adding item to list: {}, household: {}, name: {}", listId, householdId, request.name());

        // Verify membership (IDOR prevention)
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        // Get user entity
        User user = userService.getById(currentUser.id());

        UUID correlationId = getCorrelationId();

        // Add item directly
        ShoppingItem item = shoppingService.addItemDirect(
                householdId,
                listId,
                request.name(),
                request.resolvedQuantity(),
                request.unit(),
                user,
                correlationId);

        // Record activity (no commandId for direct REST)
        activityRecorder.recordShoppingItemAdded(item, user, null, correlationId);

        log.info("Item added: id={}, listId={}", item.getId(), listId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ShoppingItemDto.from(item));
    }

    @PatchMapping("/shopping-items/{itemId}")
    @Operation(
            summary = "Update a shopping item",
            description = "Updates a shopping item, typically to mark it as purchased or unpurchased.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Item updated"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    public ResponseEntity<ShoppingItemDto> updateItem(
            @PathVariable UUID householdId,
            @PathVariable UUID itemId,
            @RequestBody @Valid UpdateShoppingItemRequest request) {
        log.info("Updating item: {}, household: {}, purchased: {}", itemId, householdId, request.purchased());

        // Verify membership (IDOR prevention)
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        // Get user entity
        User user = userService.getById(currentUser.id());

        // Update item
        ShoppingItem item;
        if (Boolean.TRUE.equals(request.purchased())) {
            UUID correlationId = getCorrelationId();
            item = shoppingService.markPurchased(itemId, householdId, user, correlationId);
            // Record activity
            activityRecorder.recordShoppingItemPurchased(item, user, null, correlationId);
        } else {
            item = shoppingService.unmarkPurchased(itemId, householdId);
        }

        return ResponseEntity.ok(ShoppingItemDto.from(item));
    }

    @DeleteMapping("/shopping-items/{itemId}")
    @Operation(summary = "Delete a shopping item", description = "Deletes a shopping item from the list.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Item deleted"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "403", description = "Not a member of this household"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
    public ResponseEntity<Void> deleteItem(@PathVariable UUID householdId, @PathVariable UUID itemId) {
        log.info("Deleting item: {}, household: {}", itemId, householdId);

        // Verify membership (IDOR prevention)
        CurrentUser currentUser = userResolver.resolveCurrentUser();
        membershipService.requireMembership(currentUser.id(), householdId);

        // Get user entity
        User user = userService.getById(currentUser.id());

        // Get item details before deletion for activity recording
        ShoppingItem item = shoppingService.getItemByIdAndHouseholdId(itemId, householdId);
        String itemName = item.getName();
        UUID listId = item.getShoppingList().getId();

        // Delete item
        shoppingService.deleteItem(itemId, householdId);

        // Record activity
        UUID correlationId = getCorrelationId();
        activityRecorder.recordShoppingItemDeleted(
                itemId, itemName, listId, householdService.getById(householdId), user, correlationId);

        return ResponseEntity.noContent().build();
    }

    private UUID getCorrelationId() {
        String correlationIdStr = MDC.get(MdcKeys.CORRELATION_ID);
        if (correlationIdStr != null) {
            try {
                return UUID.fromString(correlationIdStr);
            } catch (IllegalArgumentException e) {
                // Generate new if invalid
            }
        }
        return UUID.randomUUID();
    }
}
