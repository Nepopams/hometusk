package com.hometusk.shopping.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hometusk.households.domain.Household;
import com.hometusk.users.domain.User;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShoppingRunTest {

    private Household household;
    private ShoppingList shoppingList;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        household = new Household("Test Household");
        setId(household, UUID.randomUUID());
        user = new User("ext-test", "test@example.com", "Test User");
        setId(user, UUID.randomUUID());
        shoppingList = new ShoppingList(household, "Grocery List");
        setId(shoppingList, UUID.randomUUID());
    }

    @Test
    void newRun_hasActiveStatus_andCreatedAt() {
        Instant before = Instant.now();
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        Instant after = Instant.now();

        assertThat(run.getStatus()).isEqualTo(ShoppingRunStatus.ACTIVE);
        assertThat(run.isActive()).isTrue();
        assertThat(run.getCreatedAt()).isBetween(before, after);
        assertThat(run.getClosedAt()).isNull();
        assertThat(run.getListName()).isEqualTo("Grocery List");
    }

    @Test
    void snapshotItem_copiesDataFromShoppingItem() throws Exception {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        ShoppingItem item = newItem("Milk", 2, "liters");

        ShoppingRunItem runItem = ShoppingRunItem.fromShoppingItem(run, item);

        assertThat(runItem.getName()).isEqualTo("Milk");
        assertThat(runItem.getQuantity()).isEqualTo(2);
        assertThat(runItem.getUnit()).isEqualTo("liters");
        assertThat(runItem.getCategory()).isNull();
        assertThat(runItem.getSource()).isNull();
        assertThat(runItem.getOriginalItemId()).isEqualTo(item.getId());
        assertThat(runItem.isPurchased()).isFalse();
        assertThat(runItem.getPurchasedAt()).isNull();
    }

    @Test
    void snapshotItem_copiesCategoryAndSourceFromShoppingItem() throws Exception {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        ShoppingItem item = newItem("Milk", 2, "liters");
        item.setCategory("groceries");
        item.setSource("Perekrestok");

        ShoppingRunItem runItem = ShoppingRunItem.fromShoppingItem(run, item);

        assertThat(runItem.getCategory()).isEqualTo("groceries");
        assertThat(runItem.getSource()).isEqualTo("Perekrestok");
    }

    @Test
    void closeAsCompleted_setsStatusAndClosedAt() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        Instant before = Instant.now();

        run.close(ShoppingRunStatus.COMPLETED);

        assertThat(run.getStatus()).isEqualTo(ShoppingRunStatus.COMPLETED);
        assertThat(run.isActive()).isFalse();
        assertThat(run.getClosedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void closeAsCancelled_setsStatusAndClosedAt() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);

        run.close(ShoppingRunStatus.CANCELLED);

        assertThat(run.getStatus()).isEqualTo(ShoppingRunStatus.CANCELLED);
        assertThat(run.isActive()).isFalse();
        assertThat(run.getClosedAt()).isNotNull();
    }

    @Test
    void closeAlreadyClosed_sameStatus_isIdempotent() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        run.close(ShoppingRunStatus.COMPLETED);
        Instant firstClosedAt = run.getClosedAt();

        run.close(ShoppingRunStatus.COMPLETED);

        assertThat(run.getStatus()).isEqualTo(ShoppingRunStatus.COMPLETED);
        assertThat(run.getClosedAt()).isEqualTo(firstClosedAt);
    }

    @Test
    void closeAlreadyClosed_differentStatus_throwsException() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        run.close(ShoppingRunStatus.COMPLETED);

        assertThatThrownBy(() -> run.close(ShoppingRunStatus.CANCELLED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot change status");
    }

    @Test
    void closeWithActiveStatus_throwsException() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);

        assertThatThrownBy(() -> run.close(ShoppingRunStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot close run with ACTIVE status");
    }

    @Test
    void markItemPurchased_setsTimestamp() throws Exception {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        ShoppingItem item = newItem("Bread", 1, null);
        ShoppingRunItem runItem = ShoppingRunItem.fromShoppingItem(run, item);

        Instant before = Instant.now();
        runItem.markPurchased();

        assertThat(runItem.isPurchased()).isTrue();
        assertThat(runItem.getPurchasedAt()).isAfterOrEqualTo(before);
    }

    @Test
    void markItemPurchased_isIdempotent() throws Exception {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        ShoppingItem item = newItem("Bread", 1, null);
        ShoppingRunItem runItem = ShoppingRunItem.fromShoppingItem(run, item);

        runItem.markPurchased();
        Instant firstPurchasedAt = runItem.getPurchasedAt();

        runItem.markPurchased();

        assertThat(runItem.getPurchasedAt()).isEqualTo(firstPurchasedAt);
    }

    @Test
    void unmarkItemPurchased_clearsTimestamp() throws Exception {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);
        ShoppingItem item = newItem("Bread", 1, null);
        ShoppingRunItem runItem = ShoppingRunItem.fromShoppingItem(run, item);

        runItem.markPurchased();
        runItem.unmarkPurchased();

        assertThat(runItem.isPurchased()).isFalse();
        assertThat(runItem.getPurchasedAt()).isNull();
    }

    @Test
    void getItemCounts_returnsCorrectValues() throws Exception {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);

        ShoppingItem item1 = newItem("Milk", 1, null);
        ShoppingItem item2 = newItem("Bread", 1, null);
        ShoppingItem item3 = newItem("Eggs", 1, null);

        ShoppingRunItem runItem1 = ShoppingRunItem.fromShoppingItem(run, item1);
        ShoppingRunItem runItem2 = ShoppingRunItem.fromShoppingItem(run, item2);
        ShoppingRunItem runItem3 = ShoppingRunItem.fromShoppingItem(run, item3);

        run.addItem(runItem1);
        run.addItem(runItem2);
        run.addItem(runItem3);

        runItem1.markPurchased();

        ItemCounts counts = run.getItemCounts();

        assertThat(counts.total()).isEqualTo(3);
        assertThat(counts.purchased()).isEqualTo(1);
        assertThat(counts.remaining()).isEqualTo(2);
    }

    @Test
    void getItemCounts_emptyRun_returnsZeros() {
        ShoppingRun run = new ShoppingRun(household, shoppingList, user);

        ItemCounts counts = run.getItemCounts();

        assertThat(counts.total()).isEqualTo(0);
        assertThat(counts.purchased()).isEqualTo(0);
        assertThat(counts.remaining()).isEqualTo(0);
    }

    private ShoppingItem newItem(String name, Integer quantity, String unit) throws Exception {
        ShoppingItem item = new ShoppingItem(shoppingList, name, user);
        if (quantity != null) {
            item.setQuantity(quantity);
        }
        item.setUnit(unit);
        shoppingList.addItem(item);
        setId(item, UUID.randomUUID());
        return item;
    }

    private static void setId(Object entity, UUID id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
