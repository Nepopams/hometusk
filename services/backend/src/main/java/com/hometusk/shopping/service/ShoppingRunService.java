package com.hometusk.shopping.service;

import com.hometusk.shared.exception.BusinessException;
import com.hometusk.shared.exception.ErrorCode;
import com.hometusk.shared.exception.NotFoundException;
import com.hometusk.shared.exception.ValidationException;
import com.hometusk.shopping.domain.ShoppingItem;
import com.hometusk.shopping.domain.ShoppingList;
import com.hometusk.shopping.domain.ShoppingRun;
import com.hometusk.shopping.domain.ShoppingRunItem;
import com.hometusk.shopping.domain.ShoppingRunStatus;
import com.hometusk.shopping.repository.ShoppingItemRepository;
import com.hometusk.shopping.repository.ShoppingListRepository;
import com.hometusk.shopping.repository.ShoppingRunRepository;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShoppingRunService {

    private final ShoppingRunRepository runRepository;
    private final ShoppingListRepository listRepository;
    private final ShoppingItemRepository itemRepository;
    private final UserRepository userRepository;

    public ShoppingRunService(
            ShoppingRunRepository runRepository,
            ShoppingListRepository listRepository,
            ShoppingItemRepository itemRepository,
            UserRepository userRepository) {
        this.runRepository = runRepository;
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ShoppingRun createRun(UUID householdId, UUID listId, UUID userId) {
        ShoppingList list = listRepository
                .findByIdAndHousehold_Id(listId, householdId)
                .orElseThrow(() ->
                        new NotFoundException(ErrorCode.SHOPPING_LIST_NOT_FOUND, "Shopping list not found: " + listId));

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found: " + userId));

        List<ShoppingItem> items = itemRepository.findByShoppingList_IdAndPurchasedFalseOrderByCreatedAtDesc(listId);
        if (items.isEmpty()) {
            throw new ValidationException("$.listId", "LIST_EMPTY", "Shopping list has no unpurchased items");
        }

        ShoppingRun run = new ShoppingRun(list.getHousehold(), list, user);
        items.forEach(item -> run.addItem(ShoppingRunItem.fromShoppingItem(run, item)));

        return runRepository.save(run);
    }

    @Transactional(readOnly = true)
    public List<ShoppingRun> listRuns(UUID householdId, ShoppingRunStatus status, int limit) {
        List<ShoppingRun> runs = status == null
                ? runRepository.findByHousehold_IdOrderByCreatedAtDesc(householdId)
                : runRepository.findByHousehold_IdAndStatusOrderByCreatedAtDesc(householdId, status);

        runs.forEach(this::initializeItems);
        return runs.stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public ShoppingRun getRun(UUID householdId, UUID runId) {
        ShoppingRun run = runRepository
                .findByIdAndHousehold_Id(runId, householdId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SHOPPING_RUN_NOT_FOUND,
                        "Shopping run not found: " + runId));
        initializeItems(run);
        return run;
    }

    @Transactional
    public ShoppingRun closeRun(UUID householdId, UUID runId, ShoppingRunStatus status) {
        if (status == null || status == ShoppingRunStatus.ACTIVE) {
            throw new ValidationException("$.status", "INVALID_STATUS", "Status must be COMPLETED or CANCELLED");
        }

        ShoppingRun run = getRun(householdId, runId);
        try {
            run.close(status);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("$.status", "INVALID_STATUS", e.getMessage());
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT, e.getMessage());
        }

        return runRepository.save(run);
    }

    @Transactional
    public ShoppingRunItem updateItem(
            UUID householdId,
            UUID runId,
            UUID itemId,
            boolean purchased,
            boolean syncToList) {
        ShoppingRun run = getRun(householdId, runId);
        if (!run.isActive()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Cannot update items in closed run",
                    List.of(new BusinessException.Violation("RUN_CLOSED", "Cannot update items in closed run")));
        }

        ShoppingRunItem item = run.getItems().stream()
                .filter(runItem -> runItem.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        ErrorCode.SHOPPING_ITEM_NOT_FOUND, "Shopping run item not found: " + itemId));

        if (purchased) {
            item.markPurchased();
        } else {
            item.unmarkPurchased();
        }

        if (syncToList && item.getOriginalItemId() != null) {
            Optional<ShoppingItem> original = itemRepository.findByIdAndHousehold_Id(
                    item.getOriginalItemId(), householdId);
            original.ifPresent(listItem -> {
                if (purchased) {
                    listItem.markPurchased();
                } else {
                    listItem.unmarkPurchased();
                }
                itemRepository.save(listItem);
            });
        }

        runRepository.save(run);
        return item;
    }

    private void initializeItems(ShoppingRun run) {
        run.getItems().size();
    }
}
