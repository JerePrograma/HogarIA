package com.hogaria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "monthly_plan_transaction_match")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyPlanTransactionMatch {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "monthly_plan_item_id", nullable = false)
    private UUID monthlyPlanItemId;

    @Column(name = "money_transaction_id", nullable = false)
    private UUID moneyTransactionId;

    @Column(name = "matched_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal matchedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false)
    private MatchType matchType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Confidence confidence;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum MatchType {
        MANUAL,
        SYSTEM_CONVERSION,
        SUGGESTED
    }

    public enum Confidence {
        LOW,
        MEDIUM,
        HIGH
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
    }

    @PreUpdate
    public void pu() {
        updatedAt = LocalDateTime.now();
    }
}
