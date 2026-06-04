package com.hogaria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
        name = "counterparty_alias",
        indexes = {
                @Index(name = "idx_counterparty_alias_profile_source", columnList = "profile_id, source"),
                @Index(name = "idx_counterparty_alias_document_hash", columnList = "counterparty_document_hash"),
                @Index(name = "idx_counterparty_alias_category", columnList = "default_category_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterpartyAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "source", length = 40)
    private String source;

    @Column(name = "counterparty_name", length = 255)
    private String counterpartyName;

    @Column(name = "counterparty_document_hash", length = 64)
    private String counterpartyDocumentHash;

    @Column(name = "canonical_name", nullable = false, length = 255)
    private String canonicalName;

    @Column(name = "default_category_id")
    private UUID defaultCategoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_movement_type", length = 40)
    private MoneyTransaction.MovementType defaultMovementType;

    @Builder.Default
    @Column(name = "internal_transfer_candidate", nullable = false)
    private Boolean internalTransferCandidate = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        var now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (active == null) active = true;
        if (internalTransferCandidate == null) internalTransferCandidate = false;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (active == null) active = true;
        if (internalTransferCandidate == null) internalTransferCandidate = false;
    }
}
