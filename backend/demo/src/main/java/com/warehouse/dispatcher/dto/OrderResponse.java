package com.warehouse.dispatcher.dto;

import com.warehouse.dispatcher.model.CustomerType;
import com.warehouse.dispatcher.model.Order;
import com.warehouse.dispatcher.model.OrderStatus;

import java.time.Instant;

/**
 * Outgoing payload describing an order's final/current state.
 */
public class OrderResponse {

    private String id;
    private String itemId;
    private String itemName;
    private int quantity;
    private CustomerType customerType;
    private OrderStatus status;
    private Instant createdAt;
    private String message;

    public static OrderResponse from(Order order, String itemName, String message) {
        OrderResponse r = new OrderResponse();
        r.id = order.getId();
        r.itemId = order.getItemId();
        r.itemName = itemName;
        r.quantity = order.getQuantity();
        r.customerType = order.getCustomerType();
        r.status = order.getStatus();
        r.createdAt = order.getCreatedAt();
        r.message = message;
        return r;
    }

    public String getId() {
        return id;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getMessage() {
        return message;
    }
}
