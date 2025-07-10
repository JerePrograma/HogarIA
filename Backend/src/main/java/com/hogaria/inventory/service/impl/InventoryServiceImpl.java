package com.hogaria.inventory.service.impl;

import com.hogaria.inventory.dto.*;
import com.hogaria.inventory.model.*;
import com.hogaria.inventory.repository.*;
import com.hogaria.inventory.service.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final UnitRepository unitRepo;
    private final InventoryItemRepository itemRepo;

    @Override
    public List<UnitDto> listUnits() {
        return unitRepo.findAll().stream()
                .map(u -> new UnitDto(u.getId(), u.getCodigo(), u.getDescripcion(), u.isCustom()))
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryItemDto> listItems(Long familyId, Boolean threshold) {
        List<InventoryItem> items = Boolean.TRUE.equals(threshold)
                ? itemRepo.findAllUnderThresholdByFamilyId(familyId)
                : itemRepo.findAllByFamilyId(familyId);

        return items.stream()
                .map(i -> new InventoryItemDto(
                        i.getId(),
                        i.getFamilyId(),
                        i.getUserId(),
                        i.getUnit().getId(),
                        i.getNombre(),
                        i.getQuantity(),
                        i.getMinThreshold(),
                        i.getPurchaseDate(),
                        i.getExpiryDate(),
                        i.getBarcode()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public InventoryItemDto createItem(CreateInventoryItemRequest d) {
        Unit unit = unitRepo.findById(d.unitId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Unit not found"));

        InventoryItem i = new InventoryItem();
        i.setFamilyId(d.familyId());
        i.setUserId(d.userId());
        i.setUnit(unit);
        i.setNombre(d.nombre());
        i.setQuantity(d.quantity());
        i.setMinThreshold(d.minThreshold());
        i.setPurchaseDate(d.purchaseDate());
        i.setExpiryDate(d.expiryDate());
        i.setBarcode(d.barcode());

        InventoryItem saved = itemRepo.save(i);
        return new InventoryItemDto(
                saved.getId(),
                saved.getFamilyId(),
                saved.getUserId(),
                saved.getUnit().getId(),
                saved.getNombre(),
                saved.getQuantity(),
                saved.getMinThreshold(),
                saved.getPurchaseDate(),
                saved.getExpiryDate(),
                saved.getBarcode()
        );
    }

    @Override
    public InventoryItemDto updateItem(Long id, UpdateInventoryItemRequest d) {
        InventoryItem existing = itemRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "InventoryItem not found"));

        if (d.quantity() != null) existing.setQuantity(d.quantity());
        if (d.minThreshold() != null) existing.setMinThreshold(d.minThreshold());
        if (d.purchaseDate() != null) existing.setPurchaseDate(d.purchaseDate());
        if (d.expiryDate() != null) existing.setExpiryDate(d.expiryDate());

        InventoryItem saved = itemRepo.save(existing);
        return new InventoryItemDto(
                saved.getId(),
                saved.getFamilyId(),
                saved.getUserId(),
                saved.getUnit().getId(),
                saved.getNombre(),
                saved.getQuantity(),
                saved.getMinThreshold(),
                saved.getPurchaseDate(),
                saved.getExpiryDate(),
                saved.getBarcode()
        );
    }
}
