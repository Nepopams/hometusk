package com.hometusk.shopping.repository;

import com.hometusk.shopping.domain.ShoppingRunItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShoppingRunItemRepository extends JpaRepository<ShoppingRunItem, UUID> {

    List<ShoppingRunItem> findByRun_Id(UUID runId);

    long countByRun_IdAndPurchasedTrue(UUID runId);
}
