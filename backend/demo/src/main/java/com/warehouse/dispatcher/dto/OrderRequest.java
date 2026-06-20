package com.warehouse.dispatcher.dto;

import com.warehouse.dispatcher.model.CustomerType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Incoming payload for POST /api/orders
 */
public class OrderRequest {

    @NotBlank(message = "itemId is required")
    private String itemId;

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity;

    @NotNull(message = "customerType is required (PREMIUM or REGULAR)")
    private CustomerType customerType;

    public OrderRequest() {
    }

    public OrderRequest(String itemId, int quantity, CustomerType customerType) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.customerType = customerType;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }
}
