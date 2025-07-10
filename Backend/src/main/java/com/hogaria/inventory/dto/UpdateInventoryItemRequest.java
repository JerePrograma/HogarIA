package com.hogaria.inventory.dto;


import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateInventoryItemRequest(
        BigDecimal quantity,
        BigDecimal minThreshold,
        LocalDate purchaseDate,
        LocalDate expiryDate
) {
}
