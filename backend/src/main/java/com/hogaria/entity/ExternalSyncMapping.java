package com.hogaria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
    name = "external_sync_mapping",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_external_sync_mapping_external_event_profile",
            columnNames = {
              "profile_id",
              "external_system",
              "external_entity_type",
              "external_entity_id",
              "external_event_type"
            }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalSyncMapping {
  @Id @GeneratedValue private UUID id;

  @Column(name = "profile_id", nullable = false)
  private UUID profileId;

  @Column(name = "external_system", nullable = false, length = 40)
  private String externalSystem;

  @Column(name = "external_entity_type", nullable = false, length = 40)
  private String externalEntityType;

  @Column(name = "external_entity_id", nullable = false, length = 80)
  private String externalEntityId;

  @Column(name = "external_event_type", nullable = false, length = 40)
  private String externalEventType;

  @Column(name = "money_transaction_id")
  private UUID moneyTransactionId;

  @Column(name = "monthly_plan_item_id")
  private UUID monthlyPlanItemId;

  @Column(name = "event_hash", length = 128)
  private String eventHash;

  @Column(nullable = false, length = 30)
  private String status;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Column(name = "synced_at")
  private LocalDateTime syncedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  public void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
