package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record NormalizedImportMovement(
        TransactionImportSource source,
        String sourceOperationId,
        String sourceHash,
        LocalDate realDate,
        LocalDateTime operationDateTime,
        BigDecimal amount,
        String currency,
        String rawDescription,
        String normalizedDescription,
        String extendedDescription,
        String merchantRaw,
        String merchantNormalized,
        String counterpartyRaw,
        String counterpartyDocumentHash,
        MoneyTransaction.PaymentChannel paymentChannel,
        ImportOperationKind operationKind,
        String operationType,
        String paymentMethodType,
        String paymentMethod,
        String liquidated,
        String operationTags,
        String identificationNumber,
        String payer,
        String rawPayload
) {
  public String fieldValue(String fieldName) {
    if (fieldName == null) {
      return "";
    }

    return switch (fieldName) {
      case "rawDescription" -> rawDescription;
      case "normalizedDescription" -> normalizedDescription;
      case "extendedDescription" -> extendedDescription;
      case "merchant" -> merchantRaw;
      case "merchantNormalized" -> merchantNormalized;
      case "counterparty" -> counterpartyRaw;
      case "operationType" -> operationType;
      case "paymentMethodType" -> paymentMethodType;
      case "paymentMethod" -> paymentMethod;
      case "liquidated" -> liquidated;
      case "operationTags" -> operationTags;
      case "identificationNumber" -> identificationNumber;
      case "payer" -> payer;
      default -> "";
    };
  }
}
