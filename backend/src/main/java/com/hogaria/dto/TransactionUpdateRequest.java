package com.hogaria.dto;
import com.hogaria.entity.MoneyTransaction;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;import java.time.LocalDate;import java.time.LocalDateTime;import java.util.UUID;
public record TransactionUpdateRequest(
        UUID accountId,
        UUID categoryId,
        Boolean clearCategory,
        MoneyTransaction.MovementType movementType,
        LocalDate realDate,
        LocalDate budgetDate,
        LocalDateTime operationDateTime,
        @DecimalMin("0.01") BigDecimal amount,
        @Pattern(regexp="^[A-Z]{3}$") String currency,
        String description,
        MoneyTransaction.Origin origin,
        MoneyTransaction.Status status,
        String source,
        String sourceOperationId,
        String sourceHash,
        MoneyTransaction.PaymentChannel paymentChannel,
        String counterparty,
        MoneyTransaction.ClassificationStatus classificationStatus,
        String classificationReason,
        Boolean clearInternalTransferGroup
) {}
