package com.hogaria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.UUID;

public class DevUserDtos {
  public record DevUserCreateRequest(@Email @NotBlank String email, @NotBlank String password, @NotBlank String fullName) {}
  public record DevUserResponse(UUID id, String email, String fullName, LocalDateTime createdAt) {}
}
