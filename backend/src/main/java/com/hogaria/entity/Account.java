package com.hogaria.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;
@Entity
@Table(
  name = "account",
  uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "name"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {
 @Id
 @GeneratedValue
 private UUID id;
 @Column(name = "profile_id", nullable = false)
 private UUID profileId;
 @Column(nullable = false, length = 120)
 private String name;
 @Enumerated(EnumType.STRING)
 @Column(name = "account_type", nullable = false, length = 30)
 private AccountType accountType;
 @Column(nullable = false, length = 3)
 private String currency;
 @Column(name = "credit_limit", precision = 19, scale = 2)
 private BigDecimal creditLimit;
 @Column(name = "statement_close_day")
 private Integer statementCloseDay;
 @Column(name = "due_day")
 private Integer dueDay;
 @Builder.Default
 @Column(nullable = false)
 private boolean active = true;
 public enum AccountType {
  CASH,
  BANK,
  CREDIT_CARD,
  DEBIT_CARD,
  VIRTUAL_WALLET,
  BUSINESS
 }
}