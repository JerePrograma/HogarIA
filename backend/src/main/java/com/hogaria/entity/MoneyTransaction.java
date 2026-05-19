package com.hogaria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
        name = "money_transaction",
        indexes = {
                @Index(name = "idx_money_transaction_profile_budget_date", columnList = "profile_id, budget_date"),
                @Index(name = "idx_money_transaction_profile_real_date", columnList = "profile_id, real_date"),
                @Index(name = "idx_money_transaction_profile_account_real_amount", columnList = "profile_id, account_id, real_date, amount")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyTransaction {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private MovementType movementType;

    @Column(name = "real_date", nullable = false)
    private LocalDate realDate;

    @Column(name = "budget_date", nullable = false)
    private LocalDate budgetDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Origin origin = Origin.MANUAL;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Status status = Status.CONFIRMED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum MovementType {
        INCOME,
        EXPENSE,
        SAVING,
        TRANSFER,
        ADJUSTMENT
    }

    public enum Origin {
        MANUAL,
        IMPORT,
        RECURRENT,
        SYSTEM
    }

    public enum Status {
        CONFIRMED,
        PENDING,
        IGNORED
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

        if (origin == null) {
            origin = Origin.MANUAL;
        }

        if (status == null) {
            status = Status.CONFIRMED;
        }

        if (currency != null) {
            currency = currency.toUpperCase();
        }
    }

    @PreUpdate
    public void pu() {
        updatedAt = LocalDateTime.now();

        if (currency != null) {
            currency = currency.toUpperCase();
        }
    }
}