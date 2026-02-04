package com.hometusk.shopping.domain;

import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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

    @Column(name = "list_name", nullable = false, length = 255)
    private String listName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
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

    protected ShoppingRun() {}

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
                return;
            }
            throw new IllegalStateException("Cannot change status from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
        this.closedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == ShoppingRunStatus.ACTIVE;
    }

    public void addItem(ShoppingRunItem item) {
        if (item == null) {
            return;
        }
        items.add(item);
        if (item.getRun() != this) {
            item.setRun(this);
        }
    }

    public ItemCounts getItemCounts() {
        int total = items.size();
        int purchased =
                (int) items.stream().filter(ShoppingRunItem::isPurchased).count();
        return ItemCounts.of(total, purchased);
    }

    public UUID getId() {
        return id;
    }

    public Household getHousehold() {
        return household;
    }

    public ShoppingList getSourceList() {
        return sourceList;
    }

    public String getListName() {
        return listName;
    }

    public ShoppingRunStatus getStatus() {
        return status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public List<ShoppingRunItem> getItems() {
        return items;
    }

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
