package com.hogaria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
        name = "transaction_classification_audit",
        indexes = {
                @Index(name = "idx_tx_classification_audit_transaction", columnList = "transaction_id"),
                @Index(name = "idx_tx_classification_audit_import_row", columnList = "import_row_id"),
                @Index(name = "idx_tx_classification_audit_rule", columnList = "rule_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionClassificationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "import_row_id")
    private UUID importRowId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "reason_code", nullable = false, length = 120)
    private String reasonCode;

    @Column(name = "matched_field", length = 80)
    private String matchedField;

    @Column(name = "matched_value", length = 500)
    private String matchedValue;

    @Column(name = "suggested_category_id")
    private UUID suggestedCategoryId;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
