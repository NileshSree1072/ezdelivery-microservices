package com.ezdelivery.inventory.service;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ezdelivery.common.event.Events.OrderCreated;
import com.ezdelivery.common.event.Events.OrderLineItem;
import com.ezdelivery.inventory.model.InventoryItem;
import com.ezdelivery.inventory.model.ReservationLine;
import com.ezdelivery.inventory.model.ReservationStatus;
import com.ezdelivery.inventory.model.StockReservation;
import com.ezdelivery.inventory.repository.InventoryItemRepository;
import com.ezdelivery.inventory.repository.StockReservationRepository;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryItemRepository items;
    private final StockReservationRepository reservations;

    public InventoryService(InventoryItemRepository items, StockReservationRepository reservations) {
        this.items = items;
        this.reservations = reservations;
    }

    /**
     * Reserves stock for every line of the order, all-or-nothing.
     * Idempotent: a redelivered event returns the reservation already recorded for the order.
     */
    @Transactional
    public StockReservation reserve(OrderCreated order) {
        Optional<StockReservation> existing = reservations.findByOrderId(order.orderId());
        if (existing.isPresent()) {
            log.info("Duplicate order.created for {}, reusing {} reservation", order.orderId(), existing.get().getStatus());
            return existing.get();
        }

        // Merge duplicate lines for the same product; TreeMap keeps a stable lock order.
        Map<String, Integer> requested = order.items().stream()
                .collect(Collectors.toMap(OrderLineItem::productId, OrderLineItem::quantity, Integer::sum, TreeMap::new));

        Map<String, InventoryItem> stock = items.lockAllById(requested.keySet()).stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, Function.identity()));

        for (Map.Entry<String, Integer> line : requested.entrySet()) {
            InventoryItem item = stock.get(line.getKey());
            if (item == null) {
                return reservations.save(StockReservation.rejected(order.orderId(), "No stock record for product " + line.getKey()));
            }
            if (!item.canReserve(line.getValue())) {
                return reservations.save(StockReservation.rejected(order.orderId(),
                        "Insufficient stock for product %s (requested %d, available %d)"
                                .formatted(line.getKey(), line.getValue(), item.getAvailableQuantity())));
            }
        }

        StockReservation reservation = StockReservation.reserved(order.orderId());
        requested.forEach((productId, qty) -> {
            stock.get(productId).reserve(qty);
            reservation.addLine(productId, qty);
        });
        log.info("Reserved stock for order {}: {}", order.orderId(), requested);
        return reservations.save(reservation);
    }

    /** Compensating action when payment fails. */
    @Transactional
    public void release(UUID orderId) {
        settle(orderId, InventoryItem::release, StockReservation::markReleased, "Released");
    }

    /** Payment succeeded: held units are now sold. */
    @Transactional
    public void commit(UUID orderId) {
        settle(orderId, InventoryItem::commit, StockReservation::markCommitted, "Committed");
    }

    private void settle(UUID orderId, ObjIntConsumer<InventoryItem> stockAction,
                        Consumer<StockReservation> statusAction, String verb) {
        StockReservation reservation = reservations.findByOrderId(orderId).orElse(null);
        if (reservation == null || reservation.getStatus() != ReservationStatus.RESERVED) {
            log.info("Nothing to settle for order {} (status: {})", orderId,
                    reservation == null ? "none" : reservation.getStatus());
            return;
        }
        Map<String, Integer> lines = reservation.getLines().stream()
                .collect(Collectors.toMap(ReservationLine::getProductId, ReservationLine::getQuantity, Integer::sum, TreeMap::new));
        items.lockAllById(lines.keySet()).forEach(item -> stockAction.accept(item, lines.get(item.getProductId())));
        statusAction.accept(reservation);
        log.info("{} stock for order {}", verb, orderId);
    }

    @Transactional(readOnly = true)
    public Optional<InventoryItem> find(String productId) {
        return items.findById(productId);
    }

    @Transactional
    public InventoryItem setAvailable(String productId, int quantity) {
        InventoryItem item = items.findById(productId).orElseGet(() -> new InventoryItem(productId, 0));
        item.setAvailableQuantity(quantity);
        return items.save(item);
    }
}
