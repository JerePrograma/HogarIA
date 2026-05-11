package com.hogaria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class AuthDtos {
  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
  public record LoginResponse(String token, UUID userId, String email, String fullName) {}
  public record MeResponse(UUID userId, String email, String fullName) {}
}
