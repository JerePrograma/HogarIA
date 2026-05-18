package com.hogaria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "monthly_plan_transaction_match")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyPlanTransactionMatch {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "profile_id", nullable = false)
  private UUID profileId;

  @Column(name = "monthly_plan_item_id", nullable = false)
  private UUID monthlyPlanItemId;

  @Column(name = "money_transaction_id", nullable = false)
  private UUID moneyTransactionId;

  @Column(name = "matched_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal matchedAmount;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "match_type", nullable = false, length = 30)
  private MatchType matchType = MatchType.MANUAL;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MatchConfidence confidence = MatchConfidence.HIGH;

  @Column(length = 500)
  private String note;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public enum MatchType {
    MANUAL,
    SUGGESTED_ACCEPTED,
    AUTO,
    SYSTEM_CONVERSION
  }

  public enum MatchConfidence {
    HIGH,
    MEDIUM,
    LOW
  }

  @PrePersist
  public void prePersist() {
    var now = LocalDateTime.now();

    if (createdAt == null) {
      createdAt = now;
    }

    if (updatedAt == null) {
      updatedAt = now;
    }

    if (matchType == null) {
      matchType = MatchType.MANUAL;
    }

    if (confidence == null) {
      confidence = MatchConfidence.HIGH;
    }
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
