package com.warehouse.dispatcher.controller;

import com.warehouse.dispatcher.dto.OrderRequest;
import com.warehouse.dispatcher.dto.OrderResponse;
import com.warehouse.dispatcher.model.InventoryItem;
import com.warehouse.dispatcher.model.Order;
import com.warehouse.dispatcher.model.OrderStatus;
import com.warehouse.dispatcher.repository.InventoryRepository;
import com.warehouse.dispatcher.repository.OrderRepository;
import com.warehouse.dispatcher.service.OrderDispatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderDispatchService dispatchService;
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;

    public OrderController(OrderDispatchService dispatchService,
                            OrderRepository orderRepository,
                            InventoryRepository inventoryRepository) {
        this.dispatchService = dispatchService;
        this.orderRepository = orderRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Submits a new order. Orders are queued and processed by the
     * dispatcher thread in PREMIUM-first priority order; this call blocks
     * until the order's final status is known.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        if (!inventoryRepository.existsById(request.getItemId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Order order = dispatchService.submit(request.getItemId(), request.getQuantity(), request.getCustomerType());
        InventoryItem item = inventoryRepository.findById(order.getItemId());
        String itemName = item != null ? item.getName() : "Unknown";

        String message = order.getStatus() == OrderStatus.BACKORDERED
                ? "Insufficient stock — order has been backordered."
                : "Order fulfilled successfully.";

        OrderResponse response = OrderResponse.from(order, itemName, message);

        HttpStatus status = order.getStatus() == OrderStatus.BACKORDERED
                ? HttpStatus.OK // still a valid, handled response — just a BACKORDERED outcome
                : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(response);
    }

   
    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllNewestFirst().stream()
                .map(order -> {
                    InventoryItem item = inventoryRepository.findById(order.getItemId());
                    String itemName = item != null ? item.getName() : "Unknown";
                    return OrderResponse.from(order, itemName, null);
                })
                .toList();
    }
}
