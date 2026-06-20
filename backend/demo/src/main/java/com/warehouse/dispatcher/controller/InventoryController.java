package com.warehouse.dispatcher.controller;

import com.warehouse.dispatcher.dto.InventoryItemResponse;
import com.warehouse.dispatcher.repository.InventoryRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @GetMapping
    public List<InventoryItemResponse> getAllItems() {
        return inventoryRepository.findAll().stream()
                .sorted(Comparator.comparing(item -> item.getId()))
                .map(InventoryItemResponse::from)
                .toList();
    }
}
