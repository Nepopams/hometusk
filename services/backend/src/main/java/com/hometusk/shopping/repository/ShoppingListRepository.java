package com.hometusk.shopping.repository;

import com.hometusk.shopping.domain.ShoppingList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShoppingListRepository extends JpaRepository<ShoppingList, UUID> {

    /**
     * Find list by ID scoped to household (IDOR prevention).
     */
    Optional<ShoppingList> findByIdAndHouseholdId(UUID id, UUID householdId);

    /**
     * Find list by name in a household.
     */
    Optional<ShoppingList> findByHouseholdIdAndName(UUID householdId, String name);

    /**
     * List all shopping lists in a household.
     */
    List<ShoppingList> findByHouseholdIdOrderByCreatedAtDesc(UUID householdId);

    /**
     * Find the first (default) shopping list for a household.
     */
    Optional<ShoppingList> findFirstByHouseholdIdOrderByCreatedAtAsc(UUID householdId);

    /**
     * Check if a list exists in a household.
     */
    boolean existsByIdAndHouseholdId(UUID id, UUID householdId);
}
