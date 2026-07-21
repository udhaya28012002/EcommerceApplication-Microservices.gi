package org.webapp.ecommerce.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.webapp.ecommerce.dto.response.AllInventoryResponseDto;
import org.webapp.ecommerce.dto.response.InventoryResponseDto;
import org.webapp.ecommerce.entity.Inventory;
import org.webapp.ecommerce.exception.InvalidInventoryException;
import org.webapp.ecommerce.exception.InventoryNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.webapp.ecommerce.repository.InventoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public boolean updateInventoryQuantityForOrders(Map<Long, Integer> productDetailsToBeUpdated) {
        List<Long> failedProducts = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : productDetailsToBeUpdated.entrySet()) {
            boolean success = updateInventoryQuantity(entry.getKey(), entry.getValue(), false);
            if (!success) {
                failedProducts.add(entry.getKey());
            }
        }

        if (!failedProducts.isEmpty()) {
            throw new InvalidInventoryException(
                    "Insufficient stock for productIds: " + failedProducts
            );
        }
        return true;
    }

    @Transactional
    public boolean updateInventoryQuantity(long productId, int quantity, boolean positive) {

        log.debug("Inventory update org.webapp.ecommerce.service invoked. ProductId: {}, Quantity: {}, Positive: {}", productId, quantity, positive);

        if (quantity <= 0) {

            log.warn("Invalid inventory quantity provided. Quantity: {}", quantity);

            throw new InvalidInventoryException("Quantity must be greater than 0");
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> {
                    log.warn(
                            "No inventory found for ProductId: {}", productId);
                    return new InvalidInventoryException("No inventory found for productId: " + productId);
                });

        int currentQty = inventory.getProductQuantity();
        int newQuantity;

        if (positive) {

            newQuantity = currentQty + quantity;

            log.debug("Increasing inventory quantity. ProductId: {}, CurrentQty: {}, AddedQty: {}, NewQty: {}", productId, currentQty, quantity, newQuantity);

        } else {
            if (currentQty < quantity) {

                log.warn("Insufficient inventory stock reduction attempt. ProductId: {}, CurrentQty: {}, RequestedReduction: {}", productId, currentQty, quantity);

                return false;
            }
            newQuantity = currentQty - quantity;

            log.debug("Reducing inventory quantity. ProductId: {}, CurrentQty: {}, ReducedQty: {}, NewQty: {}", productId, currentQty, quantity, newQuantity);
        }

        inventory.setProductQuantity(newQuantity);

        log.info("Inventory updated successfully. ProductId: {}, FinalQuantity: {}", productId, newQuantity);

        return true;
    }

    @Transactional
    public boolean addInventory(long productId, int productQuantity) {

        if (productQuantity <= 0) {

            log.warn("Invalid inventory quantity provided. Quantity: {}", productQuantity);

            throw new InvalidInventoryException("Quantity must be greater than 0");
        }

        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setProductQuantity(productQuantity);

        inventoryRepository.save(inventory);

        return true;
    }

    public InventoryResponseDto getInventory(long productId) {

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("No Inventory found for this ProductId : " + productId));

        return new InventoryResponseDto(productId, inventory.getInventoryId(), inventory.getProductQuantity());
    }

    public AllInventoryResponseDto getAllFromInventory(int page, int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("productId").ascending());

        Map<Long, Integer> inventory = inventoryRepository.findAllByOrderByProductIdAsc(pageable).stream()
                .collect(Collectors.toMap(
                        Inventory::getProductId,
                        Inventory::getProductQuantity
                ));

        AllInventoryResponseDto allInventoryResponseDto = new AllInventoryResponseDto();
        allInventoryResponseDto.setMapOfAllInventory(inventory);

        return allInventoryResponseDto;
    }

    public void deleteInventoryForProduct(long productId){
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("No Inventory found for this ProductId : " + productId));

        inventoryRepository.deleteById(inventory.getInventoryId());
    }

    @Transactional
    public void revertInventory(long productId, int quantityToBeReverted){
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("No Inventory found for this ProductId : " + productId));
        inventory.setProductQuantity(inventory.getProductQuantity() +  quantityToBeReverted);
    }
}
