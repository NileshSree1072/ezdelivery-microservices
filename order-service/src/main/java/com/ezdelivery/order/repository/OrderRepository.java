package com.ezdelivery.order.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ezdelivery.order.model.CustomerOrder;

public interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {

    @EntityGraph(attributePaths = "lines")
    Optional<CustomerOrder> findWithLinesById(UUID id);

    @EntityGraph(attributePaths = "lines")
    List<CustomerOrder> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
}
