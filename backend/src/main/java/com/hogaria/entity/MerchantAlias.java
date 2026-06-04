package com.hogaria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
        name = "merchant_alias",
        indexes = {
                @Index(name = "idx_merchant_alias_profile_source", columnList = "profile_id, source"),
                @Index(name = "idx_merchant_alias_normalized", columnList = "alias_normalized"),
                @Index(name = "idx_merchant_alias_category", columnList = "category_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "source", length = 40)
    private String source;

    @Column(name = "alias_raw", nullable = false, length = 255)
    private String aliasRaw;

    @Column(name = "alias_normalized", nullable = false, length = 255)
    private String aliasNormalized;

    @Column(name = "canonical_name", nullable = false, length = 255)
    private String canonicalName;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_channel", length = 40)
    private MoneyTransaction.PaymentChannel paymentChannel;

    @Column(name = "confidence", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidence;

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
