package com.hogaria.service;

import com.hogaria.dto.AuthDtos.*;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.AppUserRepository;
import com.hogaria.security.JwtService;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final AppUserRepository appUserRepository;
  private final JwtService jwtService;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public AuthService(AppUserRepository appUserRepository, JwtService jwtService) {
    this.appUserRepository = appUserRepository;
    this.jwtService = jwtService;
  }

  public LoginResponse login(LoginRequest request) {
    var user = appUserRepository.findByEmail(request.email().trim().toLowerCase()).orElseThrow(() -> new ForbiddenException("Invalid credentials"));
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) throw new ForbiddenException("Invalid credentials");
    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return new LoginResponse(token, user.getId(), user.getEmail(), user.getFullName());
  }

  public MeResponse me(UUID userId) {
    var user = appUserRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    return new MeResponse(user.getId(), user.getEmail(), user.getFullName());
  }
}
