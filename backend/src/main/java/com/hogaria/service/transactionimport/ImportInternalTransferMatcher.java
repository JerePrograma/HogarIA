package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.MoneyTransaction;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ImportInternalTransferMatcher {

    private static final BigDecimal STRONG_CONFIDENCE = new BigDecimal("1.00");
    private static final BigDecimal WEAK_CONFIDENCE = new BigDecimal("0.50");
    private static final Pattern DEBIN_COUNTERPARTY_PATTERN =
            Pattern.compile("(?i)\\bC\\s*:\\s*([0-9.\\-]+)");

    public MatchResult match(List<ImportRowSnapshot> input, Set<String> ownIdentifiers) {
        var rows = new ArrayList<>(input == null ? List.<ImportRowSnapshot>of() : input);
        var matches = new ArrayList<MatchedPair>();
        var usedDebit = new HashSet<Integer>();
        var usedCredit = new HashSet<Integer>();
        var normalizedOwnIdentifiers = normalizeOwnIdentifiers(ownIdentifiers);

        for (int debitIndex = 0; debitIndex < rows.size(); debitIndex++) {
            var debit = rows.get(debitIndex);
            if (!isBancoProvinciaOwnDebin(debit.row(), normalizedOwnIdentifiers)) {
                continue;
            }

            int creditIndex = findStrongCredit(rows, debitIndex, usedCredit);
            if (creditIndex < 0) {
                rows.set(debitIndex, debit.withRow(markPossible(debit.row(),
                        "DEBIN de cuenta propia sin contraparte MercadoPago exacta.")));
                continue;
            }

            var credit = rows.get(creditIndex);
            var operationDate = operationDate(debit.row());
            rows.set(debitIndex, debit.withRow(markMatched(debit.row())));
            rows.set(creditIndex, credit.withRow(markMatched(credit.row())));
            usedDebit.add(debitIndex);
            usedCredit.add(creditIndex);
            matches.add(new MatchedPair(
                    debit.importRowId(),
                    credit.importRowId(),
                    amount(debit.row()),
                    operationDate,
                    STRONG_CONFIDENCE,
                    "Match fuerte BP DEBITO DEBIN propio -> MercadoPago por own_id, monto y fecha operativa.",
                    true
            ));
        }

        for (int debitIndex = 0; debitIndex < rows.size(); debitIndex++) {
            if (usedDebit.contains(debitIndex) || !isBancoProvinciaDebin(rows.get(debitIndex).row())) {
                continue;
            }

            int creditIndex = findWeakCredit(rows, debitIndex, usedCredit);
            if (creditIndex < 0) {
                continue;
            }

            var debit = rows.get(debitIndex);
            var credit = rows.get(creditIndex);
            rows.set(debitIndex, debit.withRow(markPossible(debit.row(),
                    "Coincidencia débil entre fuentes por monto y fecha cercana; requiere revisión.")));
            rows.set(creditIndex, credit.withRow(markPossible(credit.row(),
                    "Coincidencia débil entre fuentes por monto y fecha cercana; requiere revisión.")));
            usedDebit.add(debitIndex);
            usedCredit.add(creditIndex);
            matches.add(new MatchedPair(
                    debit.importRowId(),
                    credit.importRowId(),
                    amount(debit.row()),
                    operationDate(debit.row()),
                    WEAK_CONFIDENCE,
                    "Match débil BP/MercadoPago por monto exacto y fecha +/- 2 días; no neutralizado.",
                    false
            ));
        }

        return new MatchResult(List.copyOf(rows), List.copyOf(matches));
    }

    private int findStrongCredit(List<ImportRowSnapshot> rows, int debitIndex, Set<Integer> usedCredit) {
        var debit = rows.get(debitIndex).row();
        for (int creditIndex = 0; creditIndex < rows.size(); creditIndex++) {
            if (usedCredit.contains(creditIndex)) {
                continue;
            }
            var credit = rows.get(creditIndex).row();
            if (isMercadoPagoFunding(credit)
                    && sameAmount(debit, credit)
                    && Objects.equals(operationDate(debit), operationDate(credit))) {
                return creditIndex;
            }
        }
        return -1;
    }

    private int findWeakCredit(List<ImportRowSnapshot> rows, int debitIndex, Set<Integer> usedCredit) {
        var debit = rows.get(debitIndex).row();
        for (int creditIndex = 0; creditIndex < rows.size(); creditIndex++) {
            if (usedCredit.contains(creditIndex)) {
                continue;
            }
            var credit = rows.get(creditIndex).row();
            if (!isMercadoPagoFunding(credit) || !sameAmount(debit, credit)) {
                continue;
            }
            var debitDate = operationDate(debit);
            var creditDate = operationDate(credit);
            if (debitDate != null && creditDate != null
                    && Math.abs(ChronoUnit.DAYS.between(debitDate, creditDate)) <= 2) {
                return creditIndex;
            }
        }
        return -1;
    }

    private boolean isBancoProvinciaOwnDebin(TransactionImportPreviewRow row, Set<String> ownIdentifiers) {
        if (!isBancoProvinciaDebin(row) || ownIdentifiers.isEmpty()) {
            return false;
        }
        var matcher = DEBIN_COUNTERPARTY_PATTERN.matcher(
                row.extendedDescription() == null ? "" : row.extendedDescription()
        );
        while (matcher.find()) {
            if (ownIdentifiers.contains(digits(matcher.group(1)))) {
                return true;
            }
        }
        return false;
    }

    private boolean isBancoProvinciaDebin(TransactionImportPreviewRow row) {
        return row != null
                && eligible(row)
                && row.source() == TransactionImportSource.BANCO_PROVINCIA
                && row.rawSignedAmount() != null
                && row.rawSignedAmount().signum() < 0
                && normalize(row.rawDescription()).contains("DEBITO DEBIN");
    }

    private boolean isMercadoPagoFunding(TransactionImportPreviewRow row) {
        if (row == null
                || !eligible(row)
                || row.source() != TransactionImportSource.MERCADO_PAGO
                || row.rawSignedAmount() == null
                || row.rawSignedAmount().signum() <= 0) {
            return false;
        }
        var text = normalize(row.rawDescription());
        return text.contains("PAGO DEBIN") || text.contains("BANK TRANSFER");
    }

    private boolean eligible(TransactionImportPreviewRow row) {
        return row.status() != RowStatus.DUPLICATE
                && row.status() != RowStatus.DUPLICATE_EXACT
                && row.status() != RowStatus.SKIPPED
                && row.status() != RowStatus.ERROR;
    }

    private TransactionImportPreviewRow markMatched(TransactionImportPreviewRow row) {
        return copy(
                row,
                MoneyTransaction.MovementType.TRANSFER,
                RowStatus.INTERNAL_TRANSFER_MATCHED,
                MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER,
                MoneyTransaction.ClassificationStatus.TECHNICAL,
                "INTERNAL_TRANSFER_MATCHED",
                "Transferencia interna confirmada por match fuerte entre fuentes.",
                null,
                null
        );
    }

    private TransactionImportPreviewRow markPossible(TransactionImportPreviewRow row, String warning) {
        if (row.status() == RowStatus.INTERNAL_TRANSFER_MATCHED) {
            return row;
        }
        return copy(
                row,
                row.movementType(),
                RowStatus.POSSIBLE_INTERNAL_TRANSFER,
                row.balanceImpact(),
                MoneyTransaction.ClassificationStatus.REVIEW,
                "POSSIBLE_INTERNAL_TRANSFER",
                warning,
                row.suggestedCategoryId(),
                row.suggestedCategoryName()
        );
    }

    private TransactionImportPreviewRow copy(
            TransactionImportPreviewRow row,
            MoneyTransaction.MovementType movementType,
            RowStatus status,
            MoneyTransaction.BalanceImpact balanceImpact,
            MoneyTransaction.ClassificationStatus classificationStatus,
            String classificationReason,
            String warning,
            UUID suggestedCategoryId,
            String suggestedCategoryName
    ) {
        return new TransactionImportPreviewRow(
                row.rowNumber(),
                row.source(),
                row.sourceOperationId(),
                row.sourceHash(),
                row.realDate(),
                row.budgetDate(),
                row.rawDescription(),
                row.normalizedDescription(),
                row.rawSignedAmount(),
                row.amount(),
                row.currency(),
                movementType,
                suggestedCategoryId,
                suggestedCategoryName,
                row.confidence(),
                status,
                warning,
                row.rawPayload(),
                row.matchedTransactionId(),
                row.matchedAccountId(),
                row.matchedCurrentCategoryId(),
                row.matchedCurrentCategoryName(),
                status.name(),
                warning,
                row.detectedFormat(),
                row.operationDateTime(),
                row.operationDateTimePrecision(),
                row.extendedDescription(),
                row.merchantName(),
                row.counterparty(),
                row.counterpartyDocumentHash(),
                row.paymentChannel(),
                balanceImpact,
                classificationStatus,
                classificationReason,
                row.classificationLayer(),
                row.classificationMatchedField(),
                row.classificationMatchedValue(),
                row.classificationExplanationJson(),
                row.categorySuggestionKey(),
                row.externalSequence(),
                row.sheetName(),
                row.targetEntity(),
                row.rawJson()
        );
    }

    private Set<String> normalizeOwnIdentifiers(Set<String> identifiers) {
        var normalized = new HashSet<String>();
        if (identifiers != null) {
            for (var identifier : identifiers) {
                var digits = digits(identifier);
                if (!digits.isBlank()) {
                    normalized.add(digits);
                }
            }
        }
        return normalized;
    }

    private LocalDate operationDate(TransactionImportPreviewRow row) {
        if (row.operationDateTime() != null) {
            return row.operationDateTime().toLocalDate();
        }
        return ImportOperationDateSupport.bancoProvinciaOperationDate(row.realDate(), row.extendedDescription());
    }

    private BigDecimal amount(TransactionImportPreviewRow row) {
        return row.amount() == null ? row.rawSignedAmount().abs() : row.amount();
    }

    private boolean sameAmount(TransactionImportPreviewRow left, TransactionImportPreviewRow right) {
        return left.amount() != null && right.amount() != null && left.amount().compareTo(right.amount()) == 0;
    }

    private String digits(String value) {
        return value == null ? "" : value.replaceAll("\\D+", "");
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record ImportRowSnapshot(UUID importRowId, UUID batchId, TransactionImportPreviewRow row) {
        ImportRowSnapshot withRow(TransactionImportPreviewRow updated) {
            return new ImportRowSnapshot(importRowId, batchId, updated);
        }
    }

    public record MatchedPair(
            UUID debitImportRowId,
            UUID creditImportRowId,
            BigDecimal amount,
            LocalDate operationDate,
            BigDecimal confidence,
            String reason,
            boolean strong
    ) {
    }

    public record MatchResult(List<ImportRowSnapshot> rows, List<MatchedPair> matches) {
    }
}
