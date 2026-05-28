package com.hogaria.dto;
import com.hogaria.entity.MoneyTransaction;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;import java.time.LocalDate;import java.time.LocalDateTime;import java.util.UUID;
public record TransactionCreateRequest(
        @NotNull UUID profileId,
        @NotNull UUID accountId,
        UUID categoryId,
        @NotNull MoneyTransaction.MovementType movementType,
        @NotNull LocalDate realDate,
        @NotNull LocalDate budgetDate,
        LocalDateTime operationDateTime,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Pattern(regexp="^[A-Z]{3}$") String currency,
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
        UUID importBatchId,
        UUID internalTransferGroupId
) {}
