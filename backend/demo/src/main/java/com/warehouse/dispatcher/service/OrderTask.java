package com.warehouse.dispatcher.service;

import com.warehouse.dispatcher.model.Order;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Wraps an Order with a CompletableFuture so the REST controller can submit
 * the order into the priority queue and then block (with a timeout) until
 * the dispatcher thread has actually processed it — giving callers a
 * synchronous response while internally honouring PREMIUM-first ordering.
 */
public class OrderTask {

    private static final AtomicLong SEQUENCE = new AtomicLong(0);

    private final Order order;
    private final long submissionSequence;
    private final CompletableFuture<Order> future = new CompletableFuture<>();

    public OrderTask(Order order) {
        this.order = order;
        this.submissionSequence = SEQUENCE.incrementAndGet();
    }

    public Order getOrder() {
        return order;
    }

    /** Used as a tiebreaker so same-priority orders are processed FIFO. */
    public long getSubmissionSequence() {
        return submissionSequence;
    }

    public CompletableFuture<Order> getFuture() {
        return future;
    }
}
