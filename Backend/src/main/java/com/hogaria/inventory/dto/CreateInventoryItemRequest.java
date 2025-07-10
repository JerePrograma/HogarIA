package com.hogaria.inventory.dto;


import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateInventoryItemRequest(
        Long familyId,
        Long userId,
        Long unitId,
        String nombre,
        BigDecimal quantity,
        BigDecimal minThreshold,
        LocalDate purchaseDate,
        LocalDate expiryDate,
        String barcode
) {
}
