package com.hogaria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "excel_import_batch")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ImportBatchStatus status;

    @Column(name = "detected_profile_type", length = 30)
    private String detectedProfileType;

    @Column(name = "year")
    private Integer year;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "summary_json", columnDefinition = "jsonb")
    private String summaryJson;

    @Column(name = "warnings_json", columnDefinition = "jsonb")
    private String warningsJson;

    @Column(name = "errors_json", columnDefinition = "jsonb")
    private String errorsJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}