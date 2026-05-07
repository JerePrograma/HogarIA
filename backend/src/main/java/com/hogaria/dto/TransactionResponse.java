package com.hogaria.dto;
import com.hogaria.entity.MoneyTransaction;import java.math.BigDecimal;import java.time.*;import java.util.UUID;
public record TransactionResponse(UUID id, UUID profileId, UUID accountId, UUID categoryId, MoneyTransaction.MovementType movementType, LocalDate realDate, LocalDate budgetDate, BigDecimal amount, String currency, String description, MoneyTransaction.Origin origin, MoneyTransaction.Status status, LocalDateTime createdAt, LocalDateTime updatedAt) {}
