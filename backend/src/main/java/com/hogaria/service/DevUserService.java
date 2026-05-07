package com.hogaria.service;

import com.hogaria.dto.DevUserDtos.*;
import com.hogaria.entity.AppUser;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.AppUserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class DevUserService {
  private final AppUserRepository repo;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  public DevUserService(AppUserRepository repo) {this.repo = repo;}
  public DevUserResponse create(DevUserCreateRequest r) {
    if (repo.existsByEmail(r.email())) throw new BadRequestException("Email already exists");
    var u = repo.save(AppUser.builder().email(r.email().trim().toLowerCase()).passwordHash(encoder.encode(r.password())).fullName(r.fullName()).build());
    return to(u);
  }
  public List<DevUserResponse> list(){ return repo.findAll().stream().map(this::to).toList(); }
  public DevUserResponse get(UUID id){ return to(repo.findById(id).orElseThrow(() -> new NotFoundException("User not found"))); }
  private DevUserResponse to(AppUser u){ return new DevUserResponse(u.getId(), u.getEmail(), u.getFullName(), u.getCreatedAt()); }
}
