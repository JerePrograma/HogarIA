package com.hogaria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "transaction_import_reference",
        indexes = {
                @Index(name = "idx_transaction_import_reference_transaction", columnList = "transaction_id"),
                @Index(name = "idx_transaction_import_reference_row", columnList = "import_row_id"),
                @Index(name = "idx_transaction_import_reference_batch", columnList = "import_batch_id"),
                @Index(name = "idx_transaction_import_reference_source_hash", columnList = "source_hash"),
                @Index(name = "idx_transaction_import_reference_source_operation", columnList = "source_operation_id"),
                @Index(name = "idx_transaction_import_reference_profile_account_source", columnList = "profile_id, account_id, import_source")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionImportReference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "import_batch_id")
    private UUID importBatchId;

    @Column(name = "import_row_id")
    private UUID importRowId;

    @Column(name = "import_source", nullable = false, length = 40)
    private String importSource;

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

    @Column(name = "counterparty_document_hash", length = 64)
    private String counterpartyDocumentHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_channel", length = 40)
    private MoneyTransaction.PaymentChannel paymentChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_status", length = 40)
    private MoneyTransaction.ClassificationStatus classificationStatus;

    @Column(name = "classification_reason", length = 255)
    private String classificationReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "classification_explanation_json", columnDefinition = "jsonb")
    private String classificationExplanationJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
