package com.hometusk.integration.shopping;

import static org.assertj.core.api.Assertions.assertThat;

import com.hometusk.households.domain.Household;
import com.hometusk.integration.IntegrationTestBase;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.domain.ShoppingRun;
import com.hometusk.shopping.domain.ShoppingRunItem;
import com.hometusk.shopping.domain.ShoppingRunStatus;
import com.hometusk.shopping.repository.ShoppingListRepository;
import com.hometusk.shopping.repository.ShoppingRunRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ShoppingRunRepositoryIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ShoppingRunRepository shoppingRunRepository;

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    private ShoppingList shoppingList;

    @BeforeEach
    void setUpList() {
        shoppingList = new ShoppingList(testHousehold, "Test Shopping List");
        shoppingList = shoppingListRepository.save(shoppingList);
    }

    @Test
    void createAndFindRun_works() {
        ShoppingRun run = new ShoppingRun(testHousehold, shoppingList, testUser);
        shoppingRunRepository.save(run);

        Optional<ShoppingRun> found = shoppingRunRepository.findByIdAndHousehold_Id(run.getId(), testHousehold.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getListName()).isEqualTo("Test Shopping List");
        assertThat(found.get().getStatus()).isEqualTo(ShoppingRunStatus.ACTIVE);
    }

    @Test
    void findByHouseholdId_returnsOnlyHouseholdRuns() {
        ShoppingRun run1 = new ShoppingRun(testHousehold, shoppingList, testUser);
        ShoppingRun run2 = new ShoppingRun(testHousehold, shoppingList, testUser);
        shoppingRunRepository.saveAll(List.of(run1, run2));

        List<ShoppingRun> runs = shoppingRunRepository.findByHousehold_IdOrderByCreatedAtDesc(testHousehold.getId());

        assertThat(runs).hasSize(2);
    }

    @Test
    void findByWrongHousehold_returnsEmpty() {
        ShoppingRun run = new ShoppingRun(testHousehold, shoppingList, testUser);
        shoppingRunRepository.save(run);

        Household otherHousehold = householdRepository.save(new Household("Other Household"));

        Optional<ShoppingRun> found =
                shoppingRunRepository.findByIdAndHousehold_Id(run.getId(), otherHousehold.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void runWithItems_cascadesProperly() {
        ShoppingRun run = new ShoppingRun(testHousehold, shoppingList, testUser);

        ShoppingItem item1 = new ShoppingItem(shoppingList, "Milk", testUser);
        item1.setQuantity(2);
        item1.setUnit("liters");
        shoppingList.addItem(item1);

        ShoppingItem item2 = new ShoppingItem(shoppingList, "Bread", testUser);
        item2.setQuantity(1);
        shoppingList.addItem(item2);

        shoppingListRepository.saveAndFlush(shoppingList);

        ShoppingRunItem runItem1 = ShoppingRunItem.fromShoppingItem(run, item1);
        ShoppingRunItem runItem2 = ShoppingRunItem.fromShoppingItem(run, item2);
        run.addItem(runItem1);
        run.addItem(runItem2);

        shoppingRunRepository.save(run);

        ShoppingRun found = shoppingRunRepository
                .findByIdAndHousehold_Id(run.getId(), testHousehold.getId())
                .orElseThrow();

        assertThat(found.getItems()).hasSize(2);
        assertThat(found.getItemCounts().total()).isEqualTo(2);
        assertThat(found.getItemCounts().purchased()).isEqualTo(0);
    }

    @Test
    void findByStatus_filtersCorrectly() {
        ShoppingRun activeRun = new ShoppingRun(testHousehold, shoppingList, testUser);
        ShoppingRun completedRun = new ShoppingRun(testHousehold, shoppingList, testUser);
        completedRun.close(ShoppingRunStatus.COMPLETED);

        shoppingRunRepository.saveAll(List.of(activeRun, completedRun));

        List<ShoppingRun> activeRuns = shoppingRunRepository.findByHousehold_IdAndStatusOrderByCreatedAtDesc(
                testHousehold.getId(), ShoppingRunStatus.ACTIVE);

        assertThat(activeRuns).hasSize(1);
        assertThat(activeRuns.get(0).getId()).isEqualTo(activeRun.getId());
    }

    @Test
    void existsByIdAndHouseholdId_works() {
        ShoppingRun run = new ShoppingRun(testHousehold, shoppingList, testUser);
        shoppingRunRepository.save(run);

        assertThat(shoppingRunRepository.existsByIdAndHousehold_Id(run.getId(), testHousehold.getId()))
                .isTrue();

        assertThat(shoppingRunRepository.existsByIdAndHousehold_Id(run.getId(), UUID.randomUUID()))
                .isFalse();
    }
}
