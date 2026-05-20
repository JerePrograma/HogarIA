package com.hogaria.entity;

import jakarta.persistence.*;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
        name = "category",
        indexes = {
                @Index(name = "idx_category_profile_id", columnList = "profile_id"),
                @Index(name = "idx_category_profile_budgetable", columnList = "profile_id, budgetable, technical")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false, length = 120)
    private String name;

    /**
     * Identidad estable para reglas de negocio.
     * No usar name para decidir reglas: name es UI, categoryKey es dominio.
     */
    @Column(name = "category_key", length = 120)
    private String categoryKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Scope scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_movement_type", length = 40)
    private MoneyTransaction.MovementType defaultMovementType;

    @Builder.Default
    @Column(nullable = false)
    private Boolean budgetable = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean technical = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum Type {
        INCOME,
        FIXED_EXPENSE,
        VARIABLE_EXPENSE,
        SAVING,
        DEBT,
        INVESTMENT
    }

    public enum Scope {
        PERSONAL,
        FAMILY,
        BUSINESS,
        GLOBAL
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

        normalizeDefaults();
    }

    @PreUpdate
    public void pu() {
        updatedAt = LocalDateTime.now();

        normalizeDefaults();
    }

    private void normalizeDefaults() {
        if (active == null) {
            active = true;
        }

        if (budgetable == null) {
            budgetable = true;
        }

        if (technical == null) {
            technical = false;
        }

        if (categoryKey == null || categoryKey.isBlank()) {
            categoryKey = buildKey(name);
        }

        if (defaultMovementType == null && type != null) {
            defaultMovementType = inferDefaultMovementType(type);
        }
    }

    private MoneyTransaction.MovementType inferDefaultMovementType(Type type) {
        if (type == Type.INCOME) {
            return MoneyTransaction.MovementType.INCOME;
        }

        if (type == Type.SAVING || type == Type.INVESTMENT) {
            return MoneyTransaction.MovementType.SAVING;
        }

        if (type == Type.DEBT) {
            return MoneyTransaction.MovementType.ADJUSTMENT;
        }

        return MoneyTransaction.MovementType.EXPENSE;
    }

    private String buildKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        var clean = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");

        return clean.isBlank() ? null : clean;
    }
}