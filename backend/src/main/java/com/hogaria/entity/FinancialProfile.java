package com.hogaria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "financial_profile")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialProfile {
    @Id @GeneratedValue
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(nullable = false)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Type type;
    @Column(name="base_currency", nullable = false, length = 3)
    private String baseCurrency;
    @Column(name="active_year", nullable = false)
    private Integer activeYear;
    @Builder.Default @Column(nullable = false)
    private Boolean active = true;
    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;
    public enum Type { PERSONAL, FAMILY, BUSINESS }
    @PrePersist public void prePersist(){var n=LocalDateTime.now(); if(createdAt==null) createdAt=n; if(updatedAt==null) updatedAt=n; if(active==null) active=true;}
    @PreUpdate public void preUpdate(){updatedAt=LocalDateTime.now();}
}
