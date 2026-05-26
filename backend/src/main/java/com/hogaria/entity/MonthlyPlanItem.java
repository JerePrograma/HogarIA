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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "monthly_plan_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyPlanItem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "account_id")
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 500)
    private String description;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "min_amount", precision = 19, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "max_amount", precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Builder.Default
    @Column(nullable = false, length = 3)
    private String currency = "ARS";

    @Column(name = "expected_recovery_amount", precision = 19, scale = 2)
    private BigDecimal expectedRecoveryAmount;

    @Column(name = "expected_recovery_percent", precision = 5, scale = 2)
    private BigDecimal expectedRecoveryPercent;

    @Column(length = 120)
    private String counterparty;

    @Column(name = "installment_number")
    private Integer installmentNumber;

    @Column(name = "installment_total")
    private Integer installmentTotal;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.IMPORTANT;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ESTIMATED;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source = Source.MANUAL;

    /**
     * Legacy compatibility link. New reconciliation state is represented by
     * monthly_plan_transaction_match, and this field is kept in sync for the
     * current frontend contract.
     */
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Type {
        INCOME,
        EXPENSE,
        SAVING,
        DEBT,
        TRANSFER,
        RECOVERY,
        TODO
    }

    public enum Priority {
        ESSENTIAL,
        IMPORTANT,
        OPTIONAL
    }

    public enum Status {
        DRAFT,
        ESTIMATED,
        SCHEDULED,
        DUE,
        PAID,
        COLLECTED,
        CANCELLED
    }

    public enum Source {
        MANUAL,
        IMPORT,
        QUICK_CAPTURE,
        SYSTEM
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

        if (currency == null) {
            currency = "ARS";
        }
    }

    @PreUpdate
    public void pu() {
        updatedAt = LocalDateTime.now();
    }
}
