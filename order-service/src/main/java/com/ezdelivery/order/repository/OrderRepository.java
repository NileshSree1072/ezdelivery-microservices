package com.ezdelivery.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ezdelivery.order.model.CustomerOrder;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {

    /**
     * Row-locks the order so saga events for the same order (which arrive on different topics and
     * threads) are applied one after another instead of failing on an optimistic-lock conflict.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") UUID id);

    @EntityGraph(attributePaths = "lines")
    Optional<CustomerOrder> findWithLinesById(UUID id);

    @EntityGraph(attributePaths = "lines")
    List<CustomerOrder> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
}
