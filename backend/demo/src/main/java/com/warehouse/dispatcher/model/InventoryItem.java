package com.warehouse.dispatcher.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a single inventory item in the warehouse.
 *
 * StockQuantity is backed by an AtomicInteger so that concurrent order
 * threads can safely reserve/decrement stock without external locking,
 * via compareAndSet-based logic in the service layer.
 */
public class InventoryItem {

    private final String id;
    private String name;
    private final AtomicInteger stockQuantity;
    private int restockThreshold;

    public InventoryItem(String id, String name, int stockQuantity, int restockThreshold) {
        this.id = id;
        this.name = name;
        this.stockQuantity = new AtomicInteger(stockQuantity);
        this.restockThreshold = restockThreshold;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStockQuantity() {
        return stockQuantity.get();
    }

    /**
     * Atomically attempts to reserve `quantity` units of stock.
     * Returns true if there was enough stock and it was reserved,
     * false if there wasn't enough stock (stock is left untouched).
     */
    public boolean tryReserve(int quantity) {
        while (true) {
            int current = stockQuantity.get();
            if (current < quantity) {
                return false;
            }
            int updated = current - quantity;
            if (stockQuantity.compareAndSet(current, updated)) {
                return true;
            }
            // someone else changed it concurrently — retry
        }
    }

    public int getRestockThreshold() {
        return restockThreshold;
    }

    public void setRestockThreshold(int restockThreshold) {
        this.restockThreshold = restockThreshold;
    }

    /** True if remaining stock is at or below the restock threshold. */
    public boolean isBelowThreshold() {
        return stockQuantity.get() <= restockThreshold;
    }
}
