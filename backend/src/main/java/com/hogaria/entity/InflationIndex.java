package com.hogaria.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "inflation_index")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InflationIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "category_code", length = 80)
    private String categoryCode;

    @Column(name = "category_name", length = 160)
    private String categoryName;

    @Column(name = "monthly_rate", nullable = false, precision = 12, scale = 6)
    private BigDecimal monthlyRate;

    @Column(name = "source", length = 120)
    private String source;

    @Column(name = "projection", nullable = false)
    private Boolean projection;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}