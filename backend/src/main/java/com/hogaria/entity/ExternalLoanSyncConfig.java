package com.hogaria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "external_loan_sync_config")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalLoanSyncConfig {
  @Id @GeneratedValue private UUID id;
  @Column(name = "profile_id", nullable = false, unique = true) private UUID profileId;
  @Column(name = "account_id", nullable = false) private UUID accountId;
  @Column(name = "loan_disbursement_category_id", nullable = false) private UUID loanDisbursementCategoryId;
  @Column(name = "principal_recovery_category_id", nullable = false) private UUID principalRecoveryCategoryId;
  @Column(name = "interest_income_category_id", nullable = false) private UUID interestIncomeCategoryId;
  @Builder.Default @Column(nullable = false) private Boolean enabled = false;
  @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

  @PrePersist public void pp(){var n=LocalDateTime.now(); if(createdAt==null) createdAt=n; if(updatedAt==null) updatedAt=n; if(enabled==null) enabled=false;}
  @PreUpdate public void pu(){updatedAt=LocalDateTime.now();}
}
