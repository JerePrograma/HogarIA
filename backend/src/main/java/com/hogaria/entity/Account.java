package com.hogaria.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import lombok.*;

@Entity @Table(name="account")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {
    @Id @GeneratedValue private UUID id;
    @Column(name="profile_id", nullable=false) private UUID profileId;
    @Column(nullable=false, length=120) private String name;
    @Column(name="account_key", length=120) private String accountKey;
    @Enumerated(EnumType.STRING) @Column(name="account_type", nullable=false, length=30) private AccountType accountType;
    @Column(nullable=false, length=3) private String currency;
    @Column(name="credit_limit", precision=19, scale=2) private BigDecimal creditLimit;
    @Column(name="statement_close_day") private Integer statementCloseDay;
    @Column(name="due_day") private Integer dueDay;
    @Builder.Default @Column(nullable=false) private Boolean active=true;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false) private LocalDateTime updatedAt;
    public enum AccountType { CASH,BANK,CREDIT_CARD,DEBIT_CARD,VIRTUAL_WALLET,BUSINESS }
    @PrePersist public void pp(){var n=LocalDateTime.now(); if(createdAt==null) createdAt=n; if(updatedAt==null) updatedAt=n; normalizeDefaults();}
    @PreUpdate public void pu(){updatedAt=LocalDateTime.now(); normalizeDefaults();}
    private void normalizeDefaults(){ if(active==null) active=true; if(currency!=null) currency=currency.toUpperCase(Locale.ROOT); if((accountKey==null || accountKey.isBlank()) && name!=null){accountKey=Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","");}}
}
