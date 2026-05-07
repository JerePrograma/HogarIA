package com.hogaria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity @Table(name="category")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {
    @Id @GeneratedValue private UUID id;
    @Column(name="profile_id") private UUID profileId;
    @Column(name="parent_id") private UUID parentId;
    @Column(nullable=false) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Type type;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Scope scope;
    @Builder.Default @Column(nullable=false) private Boolean active=true;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false) private LocalDateTime updatedAt;
    public enum Type { INCOME,FIXED_EXPENSE,VARIABLE_EXPENSE,SAVING,DEBT,INVESTMENT }
    public enum Scope { PERSONAL,FAMILY,BUSINESS,GLOBAL }
    @PrePersist public void pp(){var n=LocalDateTime.now(); if(createdAt==null) createdAt=n; if(updatedAt==null) updatedAt=n; if(active==null) active=true;}
    @PreUpdate public void pu(){updatedAt=LocalDateTime.now();}
}
