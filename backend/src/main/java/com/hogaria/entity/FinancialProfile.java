package com.hogaria.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class FinancialProfile {
  @Id @GeneratedValue private UUID id;
  private UUID userId;
  @Enumerated(EnumType.STRING) private ProfileType type;
  private String name;
  private String baseCurrency;
  private Integer activeYear;
  public enum ProfileType { PERSONAL, FAMILY, BUSINESS }
}
