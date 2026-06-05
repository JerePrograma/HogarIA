package com.hogaria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "import_internal_transfer_match",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_import_internal_transfer_match_rows",
                columnNames = {"debit_import_row_id", "credit_import_row_id"}
        ),
        indexes = {
                @Index(name = "idx_import_internal_transfer_match_profile_date", columnList = "profile_id, operation_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportInternalTransferMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "debit_import_row_id", nullable = false)
    private UUID debitImportRowId;

    @Column(name = "credit_import_row_id", nullable = false)
    private UUID creditImportRowId;

    @Column(name = "debit_transaction_id")
    private UUID debitTransactionId;

    @Column(name = "credit_transaction_id")
    private UUID creditTransactionId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "operation_date", nullable = false)
    private LocalDate operationDate;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(nullable = false, length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
