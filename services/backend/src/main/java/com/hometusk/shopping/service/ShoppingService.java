package com.hometusk.shopping.service;

import com.hometusk.households.domain.Household;
import com.hometusk.notifications.service.NotificationService;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.NotFoundException;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.repository.ShoppingItemRepository;
import com.hometusk.shopping.repository.ShoppingListRepository;
import com.hometusk.tasks.domain.Task;
import com.hometusk.tasks.repository.TaskRepository;
import com.hometusk.users.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for shopping list and item operations (Stage 5).
 */
@Service
public class ShoppingService {

    private static final Logger log = LoggerFactory.getLogger(ShoppingService.class);
    private static final String DEFAULT_LIST_NAME = "Default";

    private final ShoppingListRepository listRepository;
    private final ShoppingItemRepository itemRepository;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    public ShoppingService(
            ShoppingListRepository listRepository,
            ShoppingItemRepository itemRepository,
            TaskRepository taskRepository,
            NotificationService notificationService) {
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.taskRepository = taskRepository;
        this.notificationService = notificationService;
    }

    /**
     * Adds a shopping item with idempotency check.
     * If item with same idempotency key exists, returns existing item.
     * Safe behavior: if linkedTaskId not found, adds item without link.
     */
    @Transactional
    public ShoppingItem addItem(AddItemRequest request) {
        log.debug(
                "Adding shopping item: name={}, householdId={}, commandId={}",
                request.name(),
                request.householdId(),
                request.commandId());

        // 1. Resolve shopping list
        ShoppingList list = resolveList(request.householdId(), request.listId(), request.household());

        // 2. Resolve linked task (safe behavior: null if not found)
        Task linkedTask = resolveLinkedTask(request.linkedTaskId(), request.householdId());

        // 3. Generate idempotency key
        String idempotencyKey = ShoppingItem.generateIdempotencyKey(
                request.commandId(), list.getId(), request.name(), linkedTask != null ? linkedTask.getId() : null);

        // 4. Check for duplicate (idempotency)
        Optional<ShoppingItem> existing = itemRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info(
                    "Duplicate item detected, returning existing: idempotencyKey={}, itemId={}",
                    idempotencyKey,
                    existing.get().getId());
            return existing.get();
        }

        // 5. Create new item
        ShoppingItem item = new ShoppingItem(list, request.name(), request.addedBy());
        item.setQuantity(request.quantity() != null ? request.quantity() : 1);
        item.setUnit(request.unit());
        item.setCommandId(request.commandId());
        item.setLinkedTask(linkedTask);
        item.setIdempotencyKey(idempotencyKey);

        ShoppingItem saved = itemRepository.save(item);
        notificationService.notifyShoppingItemAdded(saved, request.addedBy(), request.correlationId());
        log.info(
                "Shopping item added: id={}, name={}, listId={}, linkedTaskId={}",
                saved.getId(),
                saved.getName(),
                list.getId(),
                linkedTask != null ? linkedTask.getId() : "null");

        return saved;
    }

    /**
     * Links multiple items to a task (used when task is created in same decision).
     */
    @Transactional
    public void linkItemsToTask(List<UUID> itemIds, UUID taskId, UUID householdId) {
        Task task = taskRepository
                .findByIdAndHousehold_Id(taskId, householdId)
                .orElseThrow(
                        () -> new NotFoundException(ErrorCode.TASK_NOT_FOUND, "Task not found for linking: " + taskId));

        for (UUID itemId : itemIds) {
            Optional<ShoppingItem> itemOpt = itemRepository.findByIdAndHousehold_Id(itemId, householdId);
            if (itemOpt.isPresent()) {
                ShoppingItem item = itemOpt.get();
                item.setLinkedTask(task);
                itemRepository.save(item);
                log.debug("Linked item {} to task {}", itemId, taskId);
            }
        }

        log.info("Linked {} items to task {}", itemIds.size(), taskId);
    }

    /**
     * Marks a shopping item as purchased.
     */
    @Transactional
    public ShoppingItem markPurchased(UUID itemId, UUID householdId, User actor, UUID correlationId) {
        ShoppingItem item = getItemByIdAndHousehold(itemId, householdId);
        boolean wasPurchased = item.isPurchased();
        item.markPurchased();
        ShoppingItem saved = itemRepository.save(item);
        if (!wasPurchased) {
            notificationService.notifyShoppingItemPurchased(saved, actor, correlationId);
        }
        log.info("Shopping item marked purchased: id={}", saved.getId());
        return saved;
    }

    /**
     * Unmarks a shopping item (not purchased).
     */
    @Transactional
    public ShoppingItem unmarkPurchased(UUID itemId, UUID householdId) {
        ShoppingItem item = getItemByIdAndHousehold(itemId, householdId);
        item.unmarkPurchased();
        return itemRepository.save(item);
    }

    /**
     * Deletes a shopping item.
     */
    @Transactional
    public void deleteItem(UUID itemId, UUID householdId) {
        ShoppingItem item = getItemByIdAndHousehold(itemId, householdId);
        itemRepository.delete(item);
        log.info("Shopping item deleted: id={}", itemId);
    }

    /**
     * Gets items linked to a specific task.
     */
    @Transactional(readOnly = true)
    public List<ShoppingItem> getItemsForTask(UUID taskId, UUID householdId) {
        // Validate task exists and belongs to household (IDOR prevention)
        if (!taskRepository.existsByIdAndHousehold_Id(taskId, householdId)) {
            throw new NotFoundException(ErrorCode.TASK_NOT_FOUND, "Task not found: " + taskId);
        }
        return itemRepository.findByLinkedTask_IdAndHouseholdId(taskId, householdId);
    }

    /**
     * Gets all items in a shopping list.
     */
    @Transactional(readOnly = true)
    public List<ShoppingItem> getItemsInList(UUID listId, UUID householdId) {
        if (!listRepository.existsByIdAndHousehold_Id(listId, householdId)) {
            throw new NotFoundException(ErrorCode.SHOPPING_LIST_NOT_FOUND, "Shopping list not found: " + listId);
        }
        return itemRepository.findByShoppingList_IdOrderByCreatedAtDesc(listId);
    }

    /**
     * Gets unpurchased items in a shopping list.
     */
    @Transactional(readOnly = true)
    public List<ShoppingItem> getUnpurchasedItemsInList(UUID listId, UUID householdId) {
        if (!listRepository.existsByIdAndHousehold_Id(listId, householdId)) {
            throw new NotFoundException(ErrorCode.SHOPPING_LIST_NOT_FOUND, "Shopping list not found: " + listId);
        }
        return itemRepository.findByShoppingList_IdAndPurchasedFalseOrderByCreatedAtDesc(listId);
    }

    /**
     * Gets all shopping lists in a household.
     */
    @Transactional(readOnly = true)
    public List<ShoppingList> getListsInHousehold(UUID householdId) {
        return listRepository.findByHousehold_IdOrderByCreatedAtDesc(householdId);
    }

    /**
     * Gets shopping list by ID (with household check).
     */
    @Transactional(readOnly = true)
    public ShoppingList getListByIdAndHousehold(UUID listId, UUID householdId) {
        return listRepository
                .findByIdAndHousehold_Id(listId, householdId)
                .orElseThrow(() ->
                        new NotFoundException(ErrorCode.SHOPPING_LIST_NOT_FOUND, "Shopping list not found: " + listId));
    }

    /**
     * Counts unpurchased items in a shopping list.
     */
    @Transactional(readOnly = true)
    public long countUnpurchasedItems(UUID listId) {
        return itemRepository.countByShoppingList_IdAndPurchasedFalse(listId);
    }

    /**
     * Adds a shopping item directly (via REST, no command/idempotency).
     * For direct user input, not AI-coordinated flows.
     */
    @Transactional
    public ShoppingItem addItemDirect(
            UUID householdId,
            UUID listId,
            String name,
            Integer quantity,
            String unit,
            User addedBy,
            UUID correlationId) {
        log.debug("Adding shopping item directly: name={}, householdId={}, listId={}", name, householdId, listId);

        // Resolve shopping list (validates exists and belongs to household)
        ShoppingList list = getListByIdAndHousehold(listId, householdId);

        // Create new item (no idempotency key for direct adds)
        ShoppingItem item = new ShoppingItem(list, name, addedBy);
        item.setQuantity(quantity != null ? quantity : 1);
        item.setUnit(unit);

        ShoppingItem saved = itemRepository.save(item);
        notificationService.notifyShoppingItemAdded(saved, addedBy, correlationId);
        log.info("Shopping item added directly: id={}, name={}, listId={}", saved.getId(), name, listId);

        return saved;
    }

    /**
     * Gets item by ID with household check (for activity recording before delete).
     */
    @Transactional(readOnly = true)
    public ShoppingItem getItemByIdAndHouseholdId(UUID itemId, UUID householdId) {
        return getItemByIdAndHousehold(itemId, householdId);
    }

    /**
     * Gets or creates the default shopping list for a household.
     */
    @Transactional
    public ShoppingList getOrCreateDefaultList(Household household) {
        return listRepository
                .findByHousehold_IdAndName(household.getId(), DEFAULT_LIST_NAME)
                .orElseGet(() -> {
                    ShoppingList list = new ShoppingList(household, DEFAULT_LIST_NAME);
                    ShoppingList saved = listRepository.save(list);
                    log.info("Created default shopping list: id={}, householdId={}", saved.getId(), household.getId());
                    return saved;
                });
    }

    // Private helpers

    private ShoppingItem getItemByIdAndHousehold(UUID itemId, UUID householdId) {
        return itemRepository
                .findByIdAndHousehold_Id(itemId, householdId)
                .orElseThrow(() ->
                        new NotFoundException(ErrorCode.SHOPPING_ITEM_NOT_FOUND, "Shopping item not found: " + itemId));
    }

    private ShoppingList resolveList(UUID householdId, UUID listId, Household household) {
        if (listId != null) {
            return listRepository
                    .findByIdAndHousehold_Id(listId, householdId)
                    .orElseThrow(() -> new NotFoundException(
                            ErrorCode.SHOPPING_LIST_NOT_FOUND, "Shopping list not found: " + listId));
        }
        // Use or create default list
        return listRepository
                .findByHousehold_IdAndName(householdId, DEFAULT_LIST_NAME)
                .orElseGet(() -> {
                    ShoppingList list = new ShoppingList(household, DEFAULT_LIST_NAME);
                    return listRepository.save(list);
                });
    }

    /**
     * Resolves linked task with safe behavior: returns null if task not found.
     * This prevents blocking item creation when task doesn't exist.
     */
    private Task resolveLinkedTask(UUID linkedTaskId, UUID householdId) {
        if (linkedTaskId == null) {
            return null;
        }
        Optional<Task> task = taskRepository.findByIdAndHousehold_Id(linkedTaskId, householdId);
        if (task.isEmpty()) {
            log.warn(
                    "Linked task not found or not in household, adding item without link: linkedTaskId={}, householdId={}",
                    linkedTaskId,
                    householdId);
            return null;
        }
        return task.get();
    }

    /**
     * Request object for adding shopping items.
     */
    public record AddItemRequest(
            UUID householdId,
            Household household,
            UUID listId,
            String name,
            Integer quantity,
            String unit,
            User addedBy,
            UUID commandId,
            UUID linkedTaskId,
            UUID correlationId) {

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private UUID householdId;
            private Household household;
            private UUID listId;
            private String name;
            private Integer quantity;
            private String unit;
            private User addedBy;
            private UUID commandId;
            private UUID linkedTaskId;
            private UUID correlationId;

            public Builder householdId(UUID householdId) {
                this.householdId = householdId;
                return this;
            }

            public Builder household(Household household) {
                this.household = household;
                return this;
            }

            public Builder listId(UUID listId) {
                this.listId = listId;
                return this;
            }

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder quantity(Integer quantity) {
                this.quantity = quantity;
                return this;
            }

            public Builder unit(String unit) {
                this.unit = unit;
                return this;
            }

            public Builder addedBy(User addedBy) {
                this.addedBy = addedBy;
                return this;
            }

            public Builder commandId(UUID commandId) {
                this.commandId = commandId;
                return this;
            }

            public Builder linkedTaskId(UUID linkedTaskId) {
                this.linkedTaskId = linkedTaskId;
                return this;
            }

            public Builder correlationId(UUID correlationId) {
                this.correlationId = correlationId;
                return this;
            }

            public AddItemRequest build() {
                return new AddItemRequest(
                        householdId,
                        household,
                        listId,
                        name,
                        quantity,
                        unit,
                        addedBy,
                        commandId,
                        linkedTaskId,
                        correlationId);
            }
        }
    }
}
