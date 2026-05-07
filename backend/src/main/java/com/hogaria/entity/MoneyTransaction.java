package com.hogaria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transaction")
public class MoneyTransaction {
  @Id @GeneratedValue private UUID id;
  private UUID profileId;
  private UUID accountId;
  private UUID categoryId;
  @Enumerated(EnumType.STRING) private MovementType movementType;
  private LocalDate realDate;
  private LocalDate budgetDate;
  @Column(precision = 19, scale = 2) private BigDecimal amount;
  private String description;
  public enum MovementType { INCOME, EXPENSE, SAVING, TRANSFER, ADJUSTMENT }
}
