package com.hogaria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "excel_import_row",
        indexes = {
                @Index(name = "idx_excel_import_row_batch_row", columnList = "batch_id, row_number"),
                @Index(name = "idx_excel_import_row_source_hash", columnList = "source_hash"),
                @Index(name = "idx_excel_import_row_source_operation", columnList = "source_operation_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "sheet_name", nullable = false, length = 120)
    private String sheetName;

    @Column(name = "row_number")
    private Integer rowNumber;

    @Column(name = "concept", length = 255)
    private String concept;

    @Column(name = "month")
    private Integer month;

    @Column(name = "real_date")
    private LocalDate realDate;

    @Column(name = "budget_date")
    private LocalDate budgetDate;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", length = 40)
    private MoneyTransaction.MovementType movementType;

    @Column(name = "source", length = 40)
    private String source;

    @Column(name = "source_operation_id", length = 120)
    private String sourceOperationId;

    @Column(name = "source_hash", length = 64)
    private String sourceHash;

    @Column(name = "external_sequence", length = 120)
    private String externalSequence;

    @Column(name = "raw_description", length = 255)
    private String rawDescription;

    @Column(name = "normalized_description", length = 500)
    private String normalizedDescription;

    @Column(name = "extended_description", length = 500)
    private String extendedDescription;

    @Column(name = "merchant_name", length = 255)
    private String merchantName;

    @Column(name = "counterparty_name", length = 255)
    private String counterpartyName;

    @Column(name = "counterparty", length = 255)
    private String counterparty;

    @Column(name = "counterparty_document_hash", length = 64)
    private String counterpartyDocumentHash;

    @Column(name = "operation_datetime")
    private LocalDateTime operationDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_datetime_precision", length = 20)
    private MoneyTransaction.OperationDateTimePrecision operationDateTimePrecision;

    @Column(name = "signed_amount", precision = 19, scale = 2)
    private BigDecimal signedAmount;

    @Column(name = "amount_abs", precision = 19, scale = 2)
    private BigDecimal amountAbs;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_channel", length = 40)
    private MoneyTransaction.PaymentChannel paymentChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_status", length = 40)
    private MoneyTransaction.ClassificationStatus classificationStatus;

    @Column(name = "classification_reason", length = 255)
    private String classificationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "balance_impact", length = 40)
    private MoneyTransaction.BalanceImpact balanceImpact;

    @Column(name = "classification_layer", length = 40)
    private String classificationLayer;

    @Column(name = "classification_matched_field", length = 80)
    private String classificationMatchedField;

    @Column(name = "classification_matched_value", length = 500)
    private String classificationMatchedValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "classification_explanation_json", columnDefinition = "jsonb")
    private String classificationExplanationJson;

    @Column(name = "suggested_category_id")
    private UUID suggestedCategoryId;

    @Column(name = "suggested_category_name", length = 255)
    private String suggestedCategoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_entity", length = 40)
    private ImportTargetEntity targetEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ImportRowStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "warning", length = 1000)
    private String warning;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", columnDefinition = "jsonb")
    private String rawJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
