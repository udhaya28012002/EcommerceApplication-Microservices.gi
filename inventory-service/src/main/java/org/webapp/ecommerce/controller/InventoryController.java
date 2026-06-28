package org.webapp.ecommerce.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.webapp.ecommerce.service.InventoryService;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PatchMapping("/internal/updateInventory/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateInventoryQuantity(@PathVariable long productId, @RequestParam int quantity, @RequestParam boolean positive){

        log.debug("Inventory update request received. ProductId: {}, Quantity: {}, Positive: {}", productId, quantity, positive);

        return ResponseEntity.ok(inventoryService.updateInventoryQuantity(productId, quantity, positive));
    }

    @PatchMapping("/internal/updateInventoryForOrder")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> updateInventoryForOrder(@RequestBody Map<Long, Integer> productDetailsToBeUpdated){

        log.debug("Inventory update request received for orders");

        return ResponseEntity.ok(inventoryService.updateInventoryQuantityForOrders(productDetailsToBeUpdated));
    }

    @GetMapping("/internal/getInventory/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    public ResponseEntity<?> getInventory(@PathVariable long productId){

        log.debug("Inventory get request received. ProductId: {}", productId);

        return ResponseEntity.ok(inventoryService.getInventory(productId));
    }

    @PatchMapping("/internal/revertInventory")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> revertInventory(@RequestParam long productId, @RequestParam int quantityToBeReverted){

        log.debug("Inventory revert request received. ProductId: {}", productId);

        inventoryService.revertInventory(productId, quantityToBeReverted);

        return ResponseEntity.ok("");
    }

    @GetMapping("/internal/getAllFromInventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllFromInventory(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size){

        log.debug("Inventory get all request received");

        return ResponseEntity.ok(inventoryService.getAllFromInventory(page, size));
    }

    @PostMapping("/internal/putInventory")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> pushIntoInventory(@RequestParam long productId, @RequestParam int quantity){

        log.debug("Inventory post request received. ProductId: {}", productId);

        return ResponseEntity.ok(inventoryService.addInventory(productId, quantity));
    }

    @DeleteMapping("/internal/deleteInventory/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteInventory(@RequestParam long productId){

        log.debug("Inventory delete request received. ProductId: {}", productId);

        inventoryService.deleteInventoryForProduct(productId);

        return ResponseEntity.ok(HttpStatus.ACCEPTED);
    }
}
