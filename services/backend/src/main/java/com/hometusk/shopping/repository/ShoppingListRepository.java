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
    Optional<ShoppingList> findByIdAndHousehold_Id(UUID id, UUID householdId);

    /**
     * Find list by name in a household.
     */
    Optional<ShoppingList> findByHousehold_IdAndName(UUID householdId, String name);

    /**
     * List all shopping lists in a household.
     */
    List<ShoppingList> findByHousehold_IdOrderByCreatedAtDesc(UUID householdId);

    /**
     * Find the first (default) shopping list for a household.
     */
    Optional<ShoppingList> findFirstByHousehold_IdOrderByCreatedAtAsc(UUID householdId);

    /**
     * Check if a list exists in a household.
     */
    boolean existsByIdAndHousehold_Id(UUID id, UUID householdId);
}
