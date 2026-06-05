package com.hogaria.entity;

import jakarta.persistence.*;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
        name = "import_counterparty_alias",
        indexes = {
                @Index(name = "idx_import_counterparty_alias_profile_active", columnList = "profile_id, active")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportCounterpartyAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "profile_id", nullable = false)
    private UUID profileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alias_type", nullable = false, length = 40)
    private AliasType aliasType;

    @Column(nullable = false, length = 255)
    private String identifier;

    @Column(name = "identifier_normalized", nullable = false, length = 255)
    private String identifierNormalized;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum AliasType {
        OWN_ID,
        OWN_NAME,
        OWN_ACCOUNT,
        INTERNAL_ACCOUNT,
        TRUSTED_EXTERNAL
    }

    @PrePersist
    void prePersist() {
        var now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        normalize();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
        normalize();
    }

    private void normalize() {
        if (active == null) active = true;
        if (identifier != null) identifier = identifier.trim();
        if (identifierNormalized == null || identifierNormalized.isBlank()) {
            identifierNormalized = normalizeIdentifier(identifier);
        }
    }

    private String normalizeIdentifier(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
