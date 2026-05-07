package com.hogaria.entity;
import jakarta.persistence.*;import java.time.LocalDateTime;import java.util.UUID;import lombok.*;
@Entity @Table(name="budget_month") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetMonth { @Id @GeneratedValue private UUID id; @Column(name="budget_year_id",nullable=false) private UUID budgetYearId; @Column(nullable=false) private Integer month; private String notes; @Column(name="created_at",nullable=false) private LocalDateTime createdAt; @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt; @PrePersist void pp(){var n=LocalDateTime.now(); if(createdAt==null)createdAt=n; if(updatedAt==null)updatedAt=n;} @PreUpdate void pu(){updatedAt=LocalDateTime.now();}}
