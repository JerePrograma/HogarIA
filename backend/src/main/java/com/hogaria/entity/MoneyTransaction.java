package com.hogaria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "money_transaction",
        indexes = {
                @Index(name = "idx_money_transaction_profile_budget_date", columnList = "profile_id, budget_date"),
                @Index(name = "idx_money_transaction_profile_real_date", columnList = "profile_id, real_date"),
                @Index(name = "idx_money_transaction_profile_account_real_amount", columnList = "profile_id, account_id, real_date, amount"),
                @Index(name = "idx_money_tx_profile_origin_real_date", columnList = "profile_id, origin, real_date"),
                @Index(name = "idx_money_tx_profile_status_budget_date", columnList = "profile_id, status, budget_date"),
                @Index(name = "idx_money_tx_profile_classification", columnList = "profile_id, classification_status, budget_date"),
                @Index(name = "idx_money_tx_profile_source_operation", columnList = "profile_id, source, source_operation_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyTransaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    /**
     * Puede ser null cuando el movimiento todavía necesita revisión.
     * Esto habilita importaciones honestas: "no sé qué categoría es" no debe convertirse
     * artificialmente en "Gastos generales".
     */
    @Column(name = "category_id")
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private MovementType movementType;

    @Column(name = "real_date", nullable = false)
    private LocalDate realDate;

    @Column(name = "budget_date", nullable = false)
    private LocalDate budgetDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 255)
    private String description;

    @Column(name = "normalized_description", length = 500)
    private String normalizedDescription;

    @Column(name = "operation_datetime")
    private LocalDateTime operationDateTime;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_datetime_precision", nullable = false, length = 20)
    private OperationDateTimePrecision operationDateTimePrecision = OperationDateTimePrecision.DATE_ONLY;

    @Column(name = "duplicate_fingerprint", length = 64)
    private String duplicateFingerprint;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "balance_impact", nullable = false, length = 40)
    private BalanceImpact balanceImpact = BalanceImpact.UNKNOWN;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Origin origin = Origin.MANUAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Status status = Status.CONFIRMED;

    @Column(name = "source", length = 40)
    private String source;

    @Column(name = "source_operation_id", length = 120)
    private String sourceOperationId;

    @Column(name = "source_hash", length = 64)
    private String sourceHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_channel", length = 40)
    private PaymentChannel paymentChannel;

    @Column(name = "counterparty", length = 255)
    private String counterparty;

    @Column(name = "counterparty_document_hash", length = 64)
    private String counterpartyDocumentHash;

    @Column(name = "external_sequence", length = 120)
    private String externalSequence;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "classification_status", nullable = false, length = 40)
    private ClassificationStatus classificationStatus = ClassificationStatus.CLASSIFIED;

    @Column(name = "classification_reason", length = 255)
    private String classificationReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "classification_explanation_json", columnDefinition = "jsonb")
    private String classificationExplanationJson;

    @Column(name = "import_batch_id")
    private UUID importBatchId;

    @Column(name = "internal_transfer_group_id")
    private UUID internalTransferGroupId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum MovementType {
        INCOME,
        EXPENSE,
        SAVING,
        TRANSFER,
        ADJUSTMENT
    }

    public enum Origin {
        MANUAL,
        IMPORT,
        RECURRENT,
        SYSTEM
    }

    public enum Status {
        CONFIRMED,
        PENDING,
        IGNORED
    }

    public enum OperationDateTimePrecision {
        DATE_ONLY,
        DATE_TIME
    }

    public enum BalanceImpact {
        OPERATING_INCOME,
        CONSUMPTION_EXPENSE,
        SAVING_OUTFLOW,
        INVESTMENT_OUTFLOW,
        DEBT_OUTFLOW,
        RECOVERABLE_OUTFLOW,
        PRINCIPAL_RECOVERY,
        INTEREST_INCOME,
        REFUND_OR_REIMBURSEMENT,
        INTERNAL_TRANSFER,
        EXTERNAL_TRANSFER,
        CASH_WITHDRAWAL,
        LOAN_ORIGINATION,
        NEUTRAL_ADJUSTMENT,
        IGNORED,
        TECHNICAL,
        UNKNOWN
    }

    public enum PaymentChannel {
        UNKNOWN,
        CASH,
        BANK_TRANSFER,
        DEBIN,
        CUENTA_DNI,
        DEBIT_CARD,
        CREDIT_CARD,
        MERCADO_PAGO,
        MERCADO_CREDITO,
        INTERNAL_TRANSFER,
        DIRECT_DEBIT,
        POS_TRANSFER,
        ATM,
        MONEY_MARKET_YIELD,
        TRANSPORT_CARD,
        QR_PAYMENT,
        CARD_FOREIGN_CURRENCY,
        OTHER
    }

    public enum ClassificationStatus {
        CLASSIFIED,
        NEEDS_CATEGORY,
        REVIEW,
        TECHNICAL,
        IGNORED_BY_RULE
    }

    @PrePersist
    public void pp() {
        var now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (origin == null) {
            origin = Origin.MANUAL;
        }

        if (status == null) {
            status = Status.CONFIRMED;
        }

        if (paymentChannel == null) {
            paymentChannel = PaymentChannel.UNKNOWN;
        }

        normalizeCurrency();
        normalizeSource();
        normalizeClassificationStatus();
        normalizeOperationDateTime();
        normalizeBalanceImpact();
    }

    @PreUpdate
    public void pu() {
        updatedAt = LocalDateTime.now();

        normalizeCurrency();
        normalizeSource();
        normalizeClassificationStatus();
        normalizeOperationDateTime();
        normalizeBalanceImpact();
    }

    private void normalizeCurrency() {
        if (currency != null) {
            currency = currency.toUpperCase(Locale.ROOT);
        }
    }

    private void normalizeSource() {
        if (source != null) {
            source = source.trim().toUpperCase(Locale.ROOT);
        }
    }

    private void normalizeClassificationStatus() {
        if (classificationStatus == null) {
            classificationStatus = categoryId == null
                    ? ClassificationStatus.NEEDS_CATEGORY
                    : ClassificationStatus.CLASSIFIED;
        }

        if (categoryId == null && classificationStatus == ClassificationStatus.CLASSIFIED) {
            classificationStatus = ClassificationStatus.NEEDS_CATEGORY;
        }
    }

    private void normalizeOperationDateTime() {
        if (operationDateTime == null && realDate != null) {
            operationDateTime = realDate.atStartOfDay();
            operationDateTimePrecision = OperationDateTimePrecision.DATE_ONLY;
        }

        if (operationDateTimePrecision == null) {
            operationDateTimePrecision = OperationDateTimePrecision.DATE_ONLY;
        }
    }

    private void normalizeBalanceImpact() {
        if (balanceImpact == null) {
            balanceImpact = BalanceImpact.UNKNOWN;
        }
    }
}
