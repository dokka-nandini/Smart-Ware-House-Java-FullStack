package com.warehouse.dispatcher.service;

import com.warehouse.dispatcher.model.CustomerType;
import com.warehouse.dispatcher.model.InventoryItem;
import com.warehouse.dispatcher.model.Order;
import com.warehouse.dispatcher.model.OrderStatus;
import com.warehouse.dispatcher.repository.InventoryRepository;
import com.warehouse.dispatcher.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Core "Smart Dispatcher" for incoming orders.
 *
 * THE TWIST — Concurrency & Priority:
 * --------------------------------------------------------------------------
 * Orders are never processed directly on the incoming HTTP thread. Instead,
 * every order is wrapped in an OrderTask and dropped onto a
 * PriorityBlockingQueue. A single dedicated dispatcher thread continuously
 * drains that queue and processes orders one at a time, in priority order:
 *
 *   1. PREMIUM orders always sort ahead of REGULAR orders.
 *   2. Within the same customer type, orders are processed FIFO (using a
 *      monotonically increasing submission sequence number as a tiebreaker).
 *
 * This means that if a burst of REGULAR and PREMIUM orders arrives at
 * (almost) the same instant, every PREMIUM order in that burst will be
 * pulled off the queue and processed before any REGULAR order that was
 * waiting at the same time — even if the REGULAR order technically arrived
 * a few milliseconds earlier.
 *
 * THREAD SAFETY:
 * --------------------------------------------------------------------------
 * - The queue itself (PriorityBlockingQueue) is thread-safe for concurrent
 *   producers (many HTTP threads submitting orders) and a single consumer.
 * - Stock is stored as an AtomicInteger inside InventoryItem and mutated
 *   via compareAndSet (see InventoryItem#tryReserve), so even if this
 *   dispatcher were scaled up to multiple consumer threads in the future,
 *   two orders could never both successfully reserve the same "last unit"
 *   of stock.
 * - The InventoryRepository's backing map is a ConcurrentHashMap.
 * - The REST controller calls submit(...) and blocks on the returned
 *   CompletableFuture (with a timeout), so the HTTP response always
 *   reflects the *actual* outcome of priority-ordered processing, not just
 *   an optimistic guess made on the calling thread.
 */
@Service
public class OrderDispatchService {

    private static final Logger log = LoggerFactory.getLogger(OrderDispatchService.class);

    // PREMIUM (ordinal 0) sorts before REGULAR (ordinal 1); ties broken by
    // submission order (FIFO) using the sequence number.
    private static final Comparator<OrderTask> PRIORITY_COMPARATOR =
            Comparator.<OrderTask, Integer>comparing(t -> t.getOrder().getCustomerType() == CustomerType.PREMIUM ? 0 : 1)
                    .thenComparing(OrderTask::getSubmissionSequence);

    private final PriorityBlockingQueue<OrderTask> queue = new PriorityBlockingQueue<>(64, PRIORITY_COMPARATOR);
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;

    private Thread dispatcherThread;
    private volatile boolean running = true;

    public OrderDispatchService(InventoryRepository inventoryRepository, OrderRepository orderRepository) {
        this.inventoryRepository = inventoryRepository;
        this.orderRepository = orderRepository;
    }

    @PostConstruct
    public void startDispatcher() {
        dispatcherThread = new Thread(this::dispatchLoop, "order-dispatcher");
        dispatcherThread.setDaemon(true);
        dispatcherThread.start();
        log.info("Order dispatcher thread started.");
    }

    @PreDestroy
    public void stopDispatcher() {
        running = false;
        if (dispatcherThread != null) {
            dispatcherThread.interrupt();
        }
    }

    /**
     * Builds an Order from the request, enqueues it for priority-based
     * processing, and blocks (with a timeout) until the dispatcher thread
     * has resolved its final status.
     */
    public Order submit(String itemId, int quantity, CustomerType customerType) {
        Order order = new Order(orderRepository.nextId(), itemId, quantity, customerType);
        orderRepository.save(order); // visible immediately as PENDING

        OrderTask task = new OrderTask(order);
        queue.put(task);

        try {
            // Wait for the dispatcher thread to actually process this order.
            return task.getFuture().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Timed out waiting for order {} to be dispatched, returning current state.", order.getId());
            return order;
        }
    }

    private void dispatchLoop() {
        while (running) {
            try {
                OrderTask task = queue.take(); // blocks until an order is available
                processOrder(task);
            } catch (InterruptedException e) {
                if (!running) {
                    break;
                }
            }
        }
    }

    private void processOrder(OrderTask task) {
        Order order = task.getOrder();
        order.setStatus(OrderStatus.PROCESSING);

        InventoryItem item = inventoryRepository.findById(order.getItemId());

        if (item == null) {
            // Unknown item — nothing to reserve against.
            order.setStatus(OrderStatus.BACKORDERED);
            orderRepository.save(order);
            task.getFuture().complete(order);
            return;
        }

        boolean reserved = item.tryReserve(order.getQuantity());

        if (reserved) {
            order.setStatus(OrderStatus.FULFILLED);
            if (item.isBelowThreshold()) {
                System.out.println("Warning: Item " + item.getName() + " has fallen below threshold!");
                log.warn("Item [{}] has fallen below threshold! Remaining stock: {}", item.getName(), item.getStockQuantity());
            }
        } else {
            // Not enough stock available to satisfy this order.
            order.setStatus(OrderStatus.BACKORDERED);
        }

        orderRepository.save(order);
        task.getFuture().complete(order);
    }
}
