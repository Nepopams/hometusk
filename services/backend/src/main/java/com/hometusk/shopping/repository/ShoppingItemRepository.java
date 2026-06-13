package com.hometusk.shopping.repository;

import com.hometusk.shopping.domain.ShoppingItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ShoppingItemRepository extends JpaRepository<ShoppingItem, UUID> {

    /**
     * Find item by ID scoped to household (IDOR prevention via list relationship).
     */
    @EntityGraph(attributePaths = {"shoppingList", "addedBy", "linkedTask"})
    @Query("SELECT i FROM ShoppingItem i WHERE i.id = :id AND i.shoppingList.household.id = :householdId")
    Optional<ShoppingItem> findByIdAndHousehold_Id(@Param("id") UUID id, @Param("householdId") UUID householdId);

    /**
     * Find items in a shopping list that are not yet purchased.
     */
    @EntityGraph(attributePaths = {"shoppingList", "addedBy", "linkedTask"})
    List<ShoppingItem> findByShoppingList_IdAndPurchasedFalseOrderByCreatedAtDesc(UUID listId);

    /**
     * Find all items in a shopping list.
     */
    @EntityGraph(attributePaths = {"shoppingList", "addedBy", "linkedTask"})
    List<ShoppingItem> findByShoppingList_IdOrderByCreatedAtDesc(UUID listId);

    /**
     * Find items in a shopping list with optional purchased/category/source filters.
     */
    @EntityGraph(attributePaths = {"shoppingList", "addedBy", "linkedTask"})
    @Query(
            """
            SELECT i FROM ShoppingItem i
            WHERE i.shoppingList.id = :listId
              AND (:purchased IS NULL OR i.purchased = :purchased)
              AND (:category IS NULL OR i.category = :category)
              AND (:source IS NULL OR i.source = :source)
            ORDER BY i.createdAt DESC
            """)
    List<ShoppingItem> findByShoppingList_IdWithFiltersOrderByCreatedAtDesc(
            @Param("listId") UUID listId,
            @Param("purchased") Boolean purchased,
            @Param("category") String category,
            @Param("source") String source);

    /**
     * Find items linked to a specific task.
     */
    @EntityGraph(attributePaths = {"shoppingList", "addedBy", "linkedTask"})
    List<ShoppingItem> findByLinkedTask_Id(UUID taskId);

    /**
     * Check if an item with this idempotency key already exists.
     * Used for duplicate prevention on retries.
     */
    boolean existsByIdempotencyKey(String idempotencyKey);

    /**
     * Find item by idempotency key.
     * Used to return existing item on retry instead of creating duplicate.
     */
    Optional<ShoppingItem> findByIdempotencyKey(String idempotencyKey);

    /**
     * Find items for a household by linked task.
     */
    @EntityGraph(attributePaths = {"shoppingList", "addedBy", "linkedTask"})
    @Query(
            "SELECT i FROM ShoppingItem i WHERE i.linkedTask.id = :taskId AND i.shoppingList.household.id = :householdId")
    List<ShoppingItem> findByLinkedTask_IdAndHouseholdId(
            @Param("taskId") UUID taskId, @Param("householdId") UUID householdId);

    /**
     * Count unpurchased items in a list.
     */
    long countByShoppingList_IdAndPurchasedFalse(UUID listId);
}
