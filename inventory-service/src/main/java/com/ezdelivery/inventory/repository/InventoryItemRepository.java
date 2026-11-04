package com.ezdelivery.inventory.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ezdelivery.inventory.model.InventoryItem;

import jakarta.persistence.LockModeType;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, String> {

    /**
     * Row-locks the items (SELECT ... FOR UPDATE) so two concurrent orders cannot both take the
     * last unit. Rows are locked in a fixed order (by productId) to avoid deadlocks.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.productId in :ids order by i.productId")
    List<InventoryItem> lockAllById(@Param("ids") Collection<String> ids);
}
