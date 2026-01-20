package com.hometusk.shopping.domain;

import com.hometusk.households.domain.Household;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shopping list container within a household.
 * Each household can have multiple named shopping lists.
 */
@Entity
@Table(name = "shopping_lists")
public class ShoppingList {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "shoppingList", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShoppingItem> items = new ArrayList<>();

    protected ShoppingList() {}

    public ShoppingList(Household household, String name) {
        this.household = household;
        this.name = name;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Household getHousehold() {
        return household;
    }

    public UUID getHouseholdId() {
        return household.getId();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ShoppingItem> getItems() {
        return items;
    }

    public void addItem(ShoppingItem item) {
        items.add(item);
        item.setShoppingList(this);
    }

    public void removeItem(ShoppingItem item) {
        items.remove(item);
        item.setShoppingList(null);
    }
}
