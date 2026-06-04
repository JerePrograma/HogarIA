package com.hogaria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
        name = "classification_rule",
        indexes = {
                @Index(name = "idx_classification_rule_profile_source", columnList = "profile_id, source"),
                @Index(name = "idx_classification_rule_priority", columnList = "priority"),
                @Index(name = "idx_classification_rule_category", columnList = "category_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationRule {

    public enum PatternType {
        EXACT,
        CONTAINS,
        REGEX
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "source", length = 40)
    private String source;

    @Column(name = "field_name", nullable = false, length = 80)
    private String fieldName;

    @Column(nullable = false, length = 500)
    private String pattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "pattern_type", nullable = false, length = 20)
    private PatternType patternType;

    @Column(name = "category_id")
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", length = 40)
    private MoneyTransaction.MovementType movementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_channel", length = 40)
    private MoneyTransaction.PaymentChannel paymentChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_status", nullable = false, length = 40)
    private MoneyTransaction.ClassificationStatus classificationStatus;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "reason_code", nullable = false, length = 120)
    private String reasonCode;

    @Column(length = 1000)
    private String warning;

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
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (active == null) active = true;
    }
}
