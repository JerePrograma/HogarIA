package com.hogaria.dto;
import com.hogaria.entity.MoneyTransaction;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;import java.time.LocalDate;import java.util.UUID;
public record TransactionUpdateRequest(UUID accountId,UUID categoryId,MoneyTransaction.MovementType movementType,LocalDate realDate,LocalDate budgetDate,@DecimalMin("0.01") BigDecimal amount,@Pattern(regexp="^[A-Z]{3}$") String currency,String description,MoneyTransaction.Origin origin,MoneyTransaction.Status status) {}
