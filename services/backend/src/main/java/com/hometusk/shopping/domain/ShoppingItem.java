package com.hometusk.shopping.domain;

import com.hometusk.tasks.domain.Task;
import com.hometusk.users.domain.User;
import jakarta.persistence.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Shopping item within a shopping list.
 * Can be optionally linked to a task (Stage 5).
 */
@Entity
@Table(name = "shopping_items")
public class ShoppingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shopping_list_id", nullable = false)
    private ShoppingList shoppingList;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "is_purchased", nullable = false)
    private boolean purchased;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_id", nullable = false)
    private User addedBy;

    @Column(name = "command_id")
    private UUID commandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_task_id")
    private Task linkedTask;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "purchased_at")
    private Instant purchasedAt;

    protected ShoppingItem() {}

    public ShoppingItem(ShoppingList shoppingList, String name, User addedBy) {
        this.shoppingList = shoppingList;
        this.name = name;
        this.addedBy = addedBy;
        this.purchased = false;
        this.quantity = 1;
        this.createdAt = Instant.now();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public ShoppingList getShoppingList() {
        return shoppingList;
    }

    public void setShoppingList(ShoppingList shoppingList) {
        this.shoppingList = shoppingList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public User getAddedBy() {
        return addedBy;
    }

    public UUID getCommandId() {
        return commandId;
    }

    public void setCommandId(UUID commandId) {
        this.commandId = commandId;
    }

    public Task getLinkedTask() {
        return linkedTask;
    }

    public UUID getLinkedTaskId() {
        return linkedTask != null ? linkedTask.getId() : null;
    }

    public void setLinkedTask(Task linkedTask) {
        this.linkedTask = linkedTask;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPurchasedAt() {
        return purchasedAt;
    }

    /**
     * Marks this item as purchased with current timestamp.
     */
    public void markPurchased() {
        this.purchased = true;
        this.purchasedAt = Instant.now();
    }

    /**
     * Marks this item as not purchased (unmark).
     */
    public void unmarkPurchased() {
        this.purchased = false;
        this.purchasedAt = null;
    }

    /**
     * Generates idempotency key for duplicate prevention.
     * Key = SHA-256(commandId + listId + normalizedName + linkedTaskId|null)
     *
     * @param commandId the command ID
     * @param listId the shopping list ID
     * @param itemName the item name (will be normalized)
     * @param linkedTaskId optional linked task ID
     * @return 64-char hex string
     */
    public static String generateIdempotencyKey(
            UUID commandId, UUID listId, String itemName, UUID linkedTaskId) {
        String normalizedName = normalizeName(itemName);
        String input =
                String.valueOf(commandId)
                        + "|"
                        + listId
                        + "|"
                        + normalizedName
                        + "|"
                        + (linkedTaskId != null ? linkedTaskId : "null");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Normalizes item name for comparison: lowercase, trimmed.
     */
    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
