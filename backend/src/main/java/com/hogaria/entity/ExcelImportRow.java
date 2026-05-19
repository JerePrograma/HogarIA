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

    @Enumerated(EnumType.STRING)
    @Column(name = "target_entity", length = 40)
    private ImportTargetEntity targetEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ImportRowStatus status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_json", columnDefinition = "jsonb")
    private String rawJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}