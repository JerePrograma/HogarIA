package com.hogaria.service.transactionimport;

import com.hogaria.dto.TransactionImportDtos.RowStatus;
import com.hogaria.dto.TransactionImportDtos.TransactionImportPreviewRow;
import com.hogaria.dto.TransactionImportDtos.TransactionImportSource;
import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.TransactionImportReferenceRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionImportDuplicateDetector {

    private final MoneyTransactionRepository txRepository;
    private final TransactionImportReferenceRepository referenceRepository;
    private final CategoryRepository categoryRepository;

    public TransactionImportDuplicateDetector(
            MoneyTransactionRepository txRepository,
            TransactionImportReferenceRepository referenceRepository,
            CategoryRepository categoryRepository
    ) {
        this.txRepository = txRepository;
        this.referenceRepository = referenceRepository;
        this.categoryRepository = categoryRepository;
    }

    public java.util.List<TransactionImportPreviewRow> applyDuplicateStatus(
            UUID profileId,
            UUID accountId,
            java.util.List<TransactionImportPreviewRow> rows
    ) {
        var seenHashes = new HashSet<String>();
        var seenOperations = new HashSet<String>();
        var resolved = new ArrayList<TransactionImportPreviewRow>();

        for (var row : rows) {
            if (row.status() == RowStatus.ERROR || row.status() == RowStatus.SKIPPED) {
                resolved.add(row);
                continue;
            }

            if (row.sourceHash() != null && !seenHashes.add(row.sourceHash())) {
                resolved.add(copyWithStatus(row, new TransactionImportMatch(
                        TransactionImportMatchType.EXACT_DUPLICATE,
                        null,
                        "Duplicado dentro del archivo: mismo source_hash."
                )));
                continue;
            }

            if (row.source() != null && row.sourceOperationId() != null) {
                var operationKey = row.source().name()
                        + "|" + row.sourceOperationId()
                        + "|" + row.realDate()
                        + "|" + amountKey(row.rawSignedAmount())
                        + "|" + row.movementType();

                if (!seenOperations.add(operationKey)) {
                    resolved.add(copyWithStatus(row, new TransactionImportMatch(
                            TransactionImportMatchType.SOURCE_DUPLICATE,
                            null,
                            "Duplicado dentro del archivo: mismo source + sourceOperationId."
                    )));
                    continue;
                }
            }

            var match = findImportMatch(profileId, accountId, row);
            resolved.add(match.found() ? copyWithStatus(row, match) : row);
        }

        return resolved;
    }

    public TransactionImportMatch findImportMatch(
            UUID profileId,
            UUID accountId,
            TransactionImportPreviewRow row
    ) {
        if (row.realDate() == null || row.amount() == null) {
            return TransactionImportMatch.none();
        }

        if (row.sourceHash() != null) {
            var activeSourceMatches = txRepository.findActiveByProfileIdAndSourceHash(
                    profileId,
                    row.sourceHash(),
                    MoneyTransaction.Status.IGNORED,
                    MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE
            );

            if (!activeSourceMatches.isEmpty()) {
                var tx = activeSourceMatches.get(0);
                return new TransactionImportMatch(
                        TransactionImportMatchType.EXACT_DUPLICATE,
                        tx.getId(),
                        "Duplicado exacto: ya existe una operación con el mismo origen/hash."
                );
            }
        }

        if (row.sourceHash() != null && row.source() != null
                && referenceRepository.findActiveByProfileIdAndAccountIdAndImportSourceAndSourceHash(
                profileId,
                accountId,
                row.source().name(),
                row.sourceHash(),
                MoneyTransaction.Status.IGNORED,
                MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE
        ).isPresent()) {
            return new TransactionImportMatch(
                    TransactionImportMatchType.EXACT_DUPLICATE,
                    null,
                    "Duplicado exacto: ya existe una referencia de importación con el mismo source_hash."
            );
        }

        if (row.source() != null && row.sourceOperationId() != null) {
            var matches = txRepository.findActiveByStrongSourceOperation(
                    profileId,
                    row.source().name(),
                    row.sourceOperationId(),
                    MoneyTransaction.Status.IGNORED,
                    MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE,
                    null
            );

            var sameFinancialRow = matches
                    .stream()
                    .filter(transaction -> Objects.equals(transaction.getRealDate(), row.realDate()))
                    .filter(transaction -> sameAmount(transaction.getAmount(), row.amount()))
                    .filter(transaction -> Objects.equals(transaction.getMovementType(), row.movementType()))
                    .findFirst();

            if (sameFinancialRow.isPresent()) {
                return new TransactionImportMatch(
                        TransactionImportMatchType.SOURCE_DUPLICATE,
                        sameFinancialRow.get().getId(),
                        "Duplicado de origen: misma operación y misma firma contable."
                );
            }
        }

        var nearby = txRepository.findByProfileIdAndRealDateBetweenAndAmount(
                profileId,
                row.realDate().minusDays(2),
                row.realDate().plusDays(2),
                row.amount()
        );

        for (var transaction : nearby) {
            if (transaction.getStatus() == MoneyTransaction.Status.IGNORED
                    || transaction.getClassificationStatus() == MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE) {
                continue;
            }

            if (Objects.equals(transaction.getAccountId(), accountId)
                    && Objects.equals(transaction.getRealDate(), row.realDate())
                    && Objects.equals(transaction.getMovementType(), row.movementType())
                    && sameSource(row, transaction)
                    && sameStrongSignature(row, transaction)) {
                return new TransactionImportMatch(
                        TransactionImportMatchType.STRONG_SAME_ACCOUNT_DUPLICATE,
                        transaction.getId(),
                        "Duplicado fuerte en misma cuenta, fuente, fecha, monto, tipo y firma normalizada."
                );
            }

            if (isBancoProvinciaMercadoPagoFundingPair(row, transaction)) {
                return new TransactionImportMatch(
                        TransactionImportMatchType.POSSIBLE_INTERNAL_TRANSFER,
                        transaction.getId(),
                        "Posible fondeo Banco Provincia ↔ Mercado Pago por monto y fecha cercana. Requiere own_id/match explícito."
                );
            }

            if (isDebitCardCrossSourceDuplicate(row, transaction)) {
                return new TransactionImportMatch(
                        TransactionImportMatchType.POSSIBLE_CROSS_SOURCE_DUPLICATE,
                        transaction.getId(),
                        "Posible duplicado Banco Provincia ↔ Mercado Pago por compra con tarjeta de débito. Revisar antes de contar ambos como consumo."
                );
            }

            if (!Objects.equals(transaction.getAccountId(), accountId)
                    && isPotentialInternalTransferText(row.normalizedDescription() + " " + transaction.getDescription())) {
                return new TransactionImportMatch(
                        TransactionImportMatchType.POSSIBLE_INTERNAL_TRANSFER,
                        transaction.getId(),
                        "Posible transferencia interna: mismo monto, fecha cercana y cuenta distinta. Se marca para revisión, no se omite automáticamente."
                );
            }
        }

        return TransactionImportMatch.none();
    }

    private TransactionImportPreviewRow copyWithStatus(
            TransactionImportPreviewRow row,
            TransactionImportMatch match
    ) {
        var resolvedStatus = switch (match.type()) {
            case EXACT_DUPLICATE -> RowStatus.DUPLICATE_EXACT;
            case POSSIBLE_INTERNAL_TRANSFER -> RowStatus.POSSIBLE_INTERNAL_TRANSFER;
            case INTERNAL_TRANSFER_MATCHED -> RowStatus.INTERNAL_TRANSFER_MATCHED;
            case POSSIBLE_CROSS_SOURCE_DUPLICATE -> RowStatus.POSSIBLE_CROSS_SOURCE_DUPLICATE;
            case SOURCE_DUPLICATE, STRONG_SAME_ACCOUNT_DUPLICATE -> RowStatus.DUPLICATE;
            case NONE -> row.status();
        };

        var matchedTransaction = match.matchedTransactionId() == null
                ? null
                : txRepository.findById(match.matchedTransactionId()).orElse(null);
        boolean confirmedInternalTransfer = match.type() == TransactionImportMatchType.INTERNAL_TRANSFER_MATCHED;
        boolean possibleInternalTransfer = match.type() == TransactionImportMatchType.POSSIBLE_INTERNAL_TRANSFER;
        boolean crossSourceRisk = match.type() == TransactionImportMatchType.POSSIBLE_CROSS_SOURCE_DUPLICATE;
        boolean keepSuggestedCategory = !confirmedInternalTransfer || isTechnicalTransferCategory(row.suggestedCategoryId());

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
                confirmedInternalTransfer ? MoneyTransaction.MovementType.TRANSFER : row.movementType(),
                keepSuggestedCategory ? row.suggestedCategoryId() : null,
                keepSuggestedCategory ? row.suggestedCategoryName() : null,
                row.confidence(),
                resolvedStatus,
                match.reason(),
                row.rawPayload(),
                match.matchedTransactionId(),
                matchedTransaction == null ? null : matchedTransaction.getAccountId(),
                matchedTransaction == null ? null : matchedTransaction.getCategoryId(),
                matchedTransaction == null || matchedTransaction.getCategoryId() == null
                        ? null
                        : categoryRepository.findById(matchedTransaction.getCategoryId()).map(Category::getName).orElse(null),
                match.type().name(),
                match.reason(),
                row.detectedFormat(),
                row.operationDateTime(),
                row.operationDateTimePrecision(),
                row.extendedDescription(),
                row.merchantName(),
                row.counterparty(),
                row.counterpartyDocumentHash(),
                row.paymentChannel(),
                confirmedInternalTransfer ? MoneyTransaction.BalanceImpact.INTERNAL_TRANSFER : row.balanceImpact(),
                confirmedInternalTransfer
                        ? MoneyTransaction.ClassificationStatus.TECHNICAL
                        : possibleInternalTransfer || crossSourceRisk
                        ? MoneyTransaction.ClassificationStatus.REVIEW
                        : row.classificationStatus(),
                match.type().isReviewOnlyRisk() ? match.type().name() : row.classificationReason(),
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

    private boolean isPotentialInternalTransferText(String raw) {
        var text = normalizeText(raw);

        return text.contains("DEBIN")
                || text.contains("PAGO DEBIN")
                || text.contains("BANK TRANSFER")
                || text.contains("TRANSFERENCIA BANCARIA")
                || text.contains("CUENTA BANCARIA DIGITAL")
                || text.contains("CUENTA DNI")
                || text.contains("TRASPASO")
                || text.contains("FONDEO")
                || text.contains("DEBITO INMEDIATO")
                || text.contains("MERCADO PAGO");
    }

    private boolean isBancoProvinciaMercadoPagoFundingPair(TransactionImportPreviewRow row, MoneyTransaction transaction) {
        var rowText = normalizeText(firstNonBlank(row.normalizedDescription(), row.rawDescription()));
        var txText = normalizeText(transaction.getDescription());
        var rowSource = row.source();
        var txSource = normalizeText(transaction.getSource());

        boolean rowIsBancoFunding = rowSource == TransactionImportSource.BANCO_PROVINCIA && isBancoProvinciaInternalFunding(rowText);
        boolean rowIsMpFunding = rowSource == TransactionImportSource.MERCADO_PAGO && isMercadoPagoInternalFunding(rowText);
        boolean txIsBancoFunding = txSource.equals("BANCO_PROVINCIA") && isBancoProvinciaInternalFunding(txText);
        boolean txIsMpFunding = txSource.equals("MERCADO_PAGO") && isMercadoPagoInternalFunding(txText);

        return (rowIsBancoFunding && txIsMpFunding) || (rowIsMpFunding && txIsBancoFunding);
    }

    private boolean isDebitCardCrossSourceDuplicate(TransactionImportPreviewRow row, MoneyTransaction transaction) {
        var rowText = normalizeText(firstNonBlank(row.normalizedDescription(), row.rawDescription()));
        var txText = normalizeText(transaction.getDescription());
        var rowSource = row.source();
        var txSource = normalizeText(transaction.getSource());

        boolean rowIsBancoDebit = rowSource == TransactionImportSource.BANCO_PROVINCIA && isBancoProvinciaDebitCardPurchase(rowText);
        boolean rowIsMpDebit = rowSource == TransactionImportSource.MERCADO_PAGO && isMercadoPagoDebitCardPurchase(rowText);
        boolean txIsBancoDebit = txSource.equals("BANCO_PROVINCIA") && isBancoProvinciaDebitCardPurchase(txText);
        boolean txIsMpDebit = txSource.equals("MERCADO_PAGO") && isMercadoPagoDebitCardPurchase(txText);

        return (rowIsBancoDebit && txIsMpDebit) || (rowIsMpDebit && txIsBancoDebit);
    }

    private boolean isBancoProvinciaInternalFunding(String value) {
        var text = normalizeText(value);

        return text.contains("DEBITO DEBIN")
                || text.contains("DB.DEBIN");
    }

    private boolean isMercadoPagoInternalFunding(String value) {
        var text = normalizeText(value);

        return text.contains("PAGO DEBIN")
                || text.contains("BANK TRANSFER")
                || text.contains("CUENTA BANCARIA DIGITAL")
                || text.contains("TRANSFERENCIA BANCARIA")
                || text.contains("DEBITO INMEDIATO");
    }

    private boolean isBancoProvinciaDebitCardPurchase(String value) {
        var text = normalizeText(value);

        return text.contains("PAGO CON TARJETA DEBITO")
                || text.contains("PAGO CON T.D.")
                || text.contains("COMPRA TARJETA DEBITO");
    }

    private boolean isMercadoPagoDebitCardPurchase(String value) {
        var text = normalizeText(value);

        return text.contains("TARJETA DE DEBITO")
                || text.contains("TARJETA DEBITO")
                || text.contains("TARJETA DE DEBITO VISA")
                || text.contains("TARJETA DEBITO VISA");
    }

    private String normalizeDescription(String value) {
        return normalizeText(value).replaceAll("\\s+", " ").trim();
    }

    private boolean sameSource(TransactionImportPreviewRow row, MoneyTransaction transaction) {
        return row.source() != null
                && transaction.getSource() != null
                && row.source().name().equalsIgnoreCase(transaction.getSource());
    }

    private boolean sameStrongSignature(TransactionImportPreviewRow row, MoneyTransaction transaction) {
        var rowSignature = normalizeDescription(firstNonBlank(row.normalizedDescription(), row.rawDescription()));
        var transactionSignature = normalizeDescription(firstNonBlank(
                transaction.getNormalizedDescription(),
                transaction.getDescription()
        ));
        return !rowSignature.isBlank() && rowSignature.equals(transactionSignature);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        var clean = value.replace('\u00A0', ' ').trim();

        clean = Normalizer.normalize(clean, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return clean.toUpperCase(Locale.ROOT);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (var value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }

        return "";
    }

    private boolean sameAmount(java.math.BigDecimal left, java.math.BigDecimal right) {
        return left != null && right != null && left.compareTo(right) == 0;
    }

    private String amountKey(java.math.BigDecimal amount) {
        return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
    }

    private boolean isTechnicalTransferCategory(UUID categoryId) {
        if (categoryId == null) {
            return false;
        }

        return categoryRepository
                .findById(categoryId)
                .filter(category -> Boolean.TRUE.equals(category.getTechnical()))
                .map(Category::getType)
                .filter(type -> type == Category.Type.SAVING
                        || type == Category.Type.INVESTMENT
                        || type == Category.Type.VARIABLE_EXPENSE)
                .isPresent();
    }
}
