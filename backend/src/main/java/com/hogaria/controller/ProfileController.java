package com.hogaria.controller;

import com.hogaria.dto.ProfileDtos.*;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.ProfileService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {
  private final ProfileService service;
  private final CurrentUserResolver userResolver;
  public ProfileController(ProfileService service, CurrentUserResolver userResolver){this.service=service;this.userResolver=userResolver;}
  @PostMapping public ProfileResponse create(@RequestHeader("X-User-Id") String h,@Valid @RequestBody ProfileCreateRequest req){ return service.create(userResolver.parse(h), req); }
  @GetMapping public List<ProfileResponse> list(@RequestHeader("X-User-Id") String h){ return service.list(userResolver.parse(h)); }
  @GetMapping("/{profileId}") public ProfileResponse get(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId){ return service.get(userResolver.parse(h), profileId); }
  @PutMapping("/{profileId}") public ProfileResponse update(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId,@Valid @RequestBody ProfileUpdateRequest req){ return service.update(userResolver.parse(h), profileId, req); }
  @DeleteMapping("/{profileId}") public void delete(@RequestHeader("X-User-Id") String h,@PathVariable UUID profileId){ service.delete(userResolver.parse(h), profileId); }
}
