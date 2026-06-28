package org.webapp.ecommerce.dto;

import java.util.Map;

public class AllInventoryResponseDto {

    private Map<Long, Integer> mapOfAllInventory;

    public Map<Long, Integer> getMapOfAllInventory() {
        return mapOfAllInventory;
    }

    public void setMapOfAllInventory(Map<Long, Integer> mapOfAllInventory) {
        this.mapOfAllInventory = mapOfAllInventory;
    }
}
