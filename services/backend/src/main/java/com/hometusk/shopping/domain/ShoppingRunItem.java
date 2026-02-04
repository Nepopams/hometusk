package com.hometusk.shopping.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "purchased", nullable = false)
    private boolean purchased;

    @Column(name = "purchased_at")
    private Instant purchasedAt;

    protected ShoppingRunItem() {}

    private ShoppingRunItem(ShoppingRun run, UUID originalItemId, String name, Integer quantity, String unit) {
        this.run = run;
        this.originalItemId = originalItemId;
        this.name = name;
        this.quantity = quantity != null ? quantity : 1;
        this.unit = unit;
        this.purchased = false;
        this.purchasedAt = null;
    }

    public static ShoppingRunItem fromShoppingItem(ShoppingRun run, ShoppingItem item) {
        return new ShoppingRunItem(run, item.getId(), item.getName(), item.getQuantity(), item.getUnit());
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

    void setRun(ShoppingRun run) {
        this.run = run;
    }

    public UUID getId() {
        return id;
    }

    public ShoppingRun getRun() {
        return run;
    }

    public UUID getOriginalItemId() {
        return originalItemId;
    }

    public String getName() {
        return name;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public Instant getPurchasedAt() {
        return purchasedAt;
    }
}
