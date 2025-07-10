package com.hogaria.inventory.service;

import com.hogaria.inventory.dto.*;

import java.util.List;

public interface InventoryService {
    List<UnitDto> listUnits();

    List<InventoryItemDto> listItems(Long familyId, Boolean threshold);

    InventoryItemDto createItem(CreateInventoryItemRequest dto);

    InventoryItemDto updateItem(Long id, UpdateInventoryItemRequest dto);
}
