package com.hogaria.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Table(name = "money_transaction")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MoneyTransaction {
@Id @GeneratedValue private UUID id;
@Column(name = "profile_id", nullable = false) private UUID profileId;
@Column(name = "account_id", nullable = false) private UUID accountId;
@Column(name = "category_id", nullable = false) private UUID categoryId;
@Enumerated(EnumType.STRING) @Column(name = "movement_type", nullable = false) private MovementType movementType;
@Column(name = "real_date", nullable = false) private LocalDate realDate;
@Column(name = "budget_date", nullable = false) private LocalDate budgetDate;
@Column(nullable = false, precision = 19, scale = 2) private BigDecimal amount;
@Column(nullable = false, length = 3) private String currency;
@Column(length = 255) private String description;
@Builder.Default @Enumerated(EnumType.STRING) @Column(nullable = false) private Origin origin = Origin.MANUAL;
@Builder.Default @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.CONFIRMED;
@Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
@Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
public enum MovementType { INCOME,EXPENSE,SAVING,TRANSFER,ADJUSTMENT }
public enum Origin { MANUAL,IMPORT,RECURRENT,SYSTEM }
public enum Status { CONFIRMED,PENDING,IGNORED }
@PrePersist public void pp(){var n=LocalDateTime.now(); if(createdAt==null) createdAt=n; if(updatedAt==null) updatedAt=n;}
@PreUpdate public void pu(){updatedAt=LocalDateTime.now();}
}
