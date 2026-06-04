package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.Confidence;
import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.entity.MoneyTransaction;

final class ImportClassificationResults {

  private ImportClassificationResults() {
  }

  static ImportClassificationResult result(
          NormalizedImportMovement movement,
          ClassificationLayer layer,
          String matchedField,
          String matchedValue,
          MoneyTransaction.MovementType movementType,
          MoneyTransaction.BalanceImpact balanceImpact,
          MoneyTransaction.PaymentChannel paymentChannel,
          String categoryKey,
          String categoryName,
          MoneyTransaction.ClassificationStatus classificationStatus,
          String reasonCode,
          Confidence confidence,
          RowStatus rowStatus,
          String warning
  ) {
    return new ImportClassificationResult(
            movementType,
            balanceImpact,
            paymentChannel == null ? movement.paymentChannel() : paymentChannel,
            categoryKey,
            categoryName,
            classificationStatus,
            reasonCode,
            confidence,
            rowStatus,
            warning,
            layer,
            matchedField,
            matchedValue,
            explanationJson(layer, matchedField, matchedValue, categoryKey, categoryName, classificationStatus, reasonCode, confidence, rowStatus, warning)
    );
  }

  static ImportClassificationResult expense(
          NormalizedImportMovement movement,
          ClassificationLayer layer,
          String matchedField,
          String matchedValue,
          MoneyTransaction.PaymentChannel paymentChannel,
          String categoryKey,
          String categoryName,
          String reasonCode,
          Confidence confidence
  ) {
    return result(
            movement,
            layer,
            matchedField,
            matchedValue,
            MoneyTransaction.MovementType.EXPENSE,
            MoneyTransaction.BalanceImpact.CONSUMPTION_EXPENSE,
            paymentChannel,
            categoryKey,
            categoryName,
            MoneyTransaction.ClassificationStatus.CLASSIFIED,
            reasonCode,
            confidence,
            RowStatus.READY,
            null
    );
  }

  static ImportClassificationResult review(
          NormalizedImportMovement movement,
          ClassificationLayer layer,
          String matchedField,
          String matchedValue,
          MoneyTransaction.MovementType movementType,
          MoneyTransaction.BalanceImpact balanceImpact,
          MoneyTransaction.PaymentChannel paymentChannel,
          String categoryKey,
          String categoryName,
          String reasonCode,
          Confidence confidence,
          String warning
  ) {
    return result(
            movement,
            layer,
            matchedField,
            matchedValue,
            movementType,
            balanceImpact,
            paymentChannel,
            categoryKey,
            categoryName,
            MoneyTransaction.ClassificationStatus.REVIEW,
            reasonCode,
            confidence,
            RowStatus.REVIEW,
            warning
    );
  }

  private static String explanationJson(
          ClassificationLayer layer,
          String matchedField,
          String matchedValue,
          String categoryKey,
          String categoryName,
          MoneyTransaction.ClassificationStatus classificationStatus,
          String reasonCode,
          Confidence confidence,
          RowStatus rowStatus,
          String warning
  ) {
    return "{"
            + "\"reasonCode\":\"" + escape(reasonCode) + "\","
            + "\"layer\":\"" + escape(layer == null ? null : layer.name()) + "\","
            + "\"matchedField\":\"" + escape(matchedField) + "\","
            + "\"matchedValue\":\"" + escape(matchedValue) + "\","
            + "\"categoryKey\":\"" + escape(categoryKey) + "\","
            + "\"categoryName\":\"" + escape(categoryName) + "\","
            + "\"confidence\":\"" + escape(confidence == null ? null : confidence.name()) + "\","
            + "\"classificationStatus\":\"" + escape(classificationStatus == null ? null : classificationStatus.name()) + "\","
            + "\"rowStatus\":\"" + escape(rowStatus == null ? null : rowStatus.name()) + "\","
            + "\"autoClassified\":" + (classificationStatus == MoneyTransaction.ClassificationStatus.CLASSIFIED && rowStatus == RowStatus.READY)
            + (warning == null || warning.isBlank() ? "" : ",\"warning\":\"" + escape(warning) + "\"")
            + "}";
  }

  private static String escape(String value) {
    return value == null
            ? ""
            : value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", " ")
            .replace("\n", " ");
  }
}
