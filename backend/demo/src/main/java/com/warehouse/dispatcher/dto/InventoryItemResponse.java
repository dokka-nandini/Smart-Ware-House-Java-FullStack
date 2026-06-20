package com.warehouse.dispatcher.dto;

import com.warehouse.dispatcher.model.InventoryItem;

public class InventoryItemResponse {

    private String id;
    private String name;
    private int stockQuantity;
    private int restockThreshold;
    private boolean belowThreshold;

    public static InventoryItemResponse from(InventoryItem item) {
        InventoryItemResponse r = new InventoryItemResponse();
        r.id = item.getId();
        r.name = item.getName();
        r.stockQuantity = item.getStockQuantity();
        r.restockThreshold = item.getRestockThreshold();
        r.belowThreshold = item.isBelowThreshold();
        return r;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public int getRestockThreshold() {
        return restockThreshold;
    }

    public boolean isBelowThreshold() {
        return belowThreshold;
    }
}
