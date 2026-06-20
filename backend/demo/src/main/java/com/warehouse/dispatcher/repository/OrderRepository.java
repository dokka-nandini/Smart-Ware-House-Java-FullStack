package com.warehouse.dispatcher.repository;

import com.warehouse.dispatcher.model.Order;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class OrderRepository {

    private final ConcurrentHashMap<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public String nextId() {
        return "ORD-" + sequence.incrementAndGet();
    }

    public void save(Order order) {
        orders.put(order.getId(), order);
    }

    public List<Order> findAllNewestFirst() {
        List<Order> all = new ArrayList<>(orders.values());
        all.sort(Comparator.comparing(Order::getCreatedAt).reversed());
        return all;
    }
}
