package com.warehouse.dispatcher.repository;

import com.warehouse.dispatcher.model.InventoryItem;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for InventoryItems.
 *
 * ConcurrentHashMap gives us thread-safe reads/writes to the map itself
 * (adding/removing/looking-up items). The actual stock decrement race
 * condition is handled inside InventoryItem via AtomicInteger.compareAndSet,
 * since two threads could otherwise both "see" enough stock and both
 * proceed to fulfil an order that should have been BACKORDERED.
 */
@Repository
public class InventoryRepository {

    private final ConcurrentHashMap<String, InventoryItem> items = new ConcurrentHashMap<>();

    @PostConstruct
    public void seed() {
        items.put("ITEM-001", new InventoryItem("ITEM-001", "Wireless Mouse", 50, 10));
        items.put("ITEM-002", new InventoryItem("ITEM-002", "Mechanical Keyboard", 20, 5));
        items.put("ITEM-003", new InventoryItem("ITEM-003", "USB-C Hub", 8, 5));
    }

    public Collection<InventoryItem> findAll() {
        return items.values();
    }

    public InventoryItem findById(String id) {
        return items.get(id);
    }

    public boolean existsById(String id) {
        return items.containsKey(id);
    }

    public void save(InventoryItem item) {
        items.put(item.getId(), item);
    }
}
