package com.ezdelivery.order.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    /** Title and price are copied at order time, so later catalog edits don't change past orders. */
    @Column(name = "product_title", nullable = false, length = 200)
    private String productTitle;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    protected OrderLine() {
    }

    OrderLine(CustomerOrder order, String productId, String productTitle, BigDecimal unitPrice, int quantity) {
        this.order = order;
        this.productId = productId;
        this.productTitle = productTitle;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public String getProductId() { return productId; }
    public String getProductTitle() { return productTitle; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
