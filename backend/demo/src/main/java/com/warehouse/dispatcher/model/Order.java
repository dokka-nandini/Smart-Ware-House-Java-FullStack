package com.warehouse.dispatcher.model;

import java.time.Instant;

/**
 * Represents a customer order for a single inventory item.
 */
public class Order {

    private final String id;
    private final String itemId;
    private final int quantity;
    private final CustomerType customerType;
    private volatile OrderStatus status;
    private final Instant createdAt;

    public Order(String id, String itemId, int quantity, CustomerType customerType) {
        this.id = id;
        this.itemId = itemId;
        this.quantity = quantity;
        this.customerType = customerType;
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
