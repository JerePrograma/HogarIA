package com.hogaria.entity;
import jakarta.persistence.*;import lombok.*;import java.time.LocalDateTime;import java.util.UUID;
@Entity @Table(name="app_user") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppUser { @Id @GeneratedValue private UUID id; @Column(nullable=false,unique=true) private String email; @Column(nullable=false) private String passwordHash; @Column(nullable=false) private String fullName; @Column(nullable=false) private LocalDateTime createdAt; }
