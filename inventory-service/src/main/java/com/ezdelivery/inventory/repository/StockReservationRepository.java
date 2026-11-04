package com.ezdelivery.inventory.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ezdelivery.inventory.model.StockReservation;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    @EntityGraph(attributePaths = "lines")
    Optional<StockReservation> findByOrderId(UUID orderId);
}
