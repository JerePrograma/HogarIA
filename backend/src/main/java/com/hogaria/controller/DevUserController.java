package com.hogaria.controller;

import com.hogaria.dto.DevUserDtos.*;
import com.hogaria.service.DevUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dev/users")
public class DevUserController {
  private final DevUserService service;
  public DevUserController(DevUserService service){this.service=service;}
  @PostMapping public DevUserResponse create(@Valid @RequestBody DevUserCreateRequest req){ return service.create(req); }
  @GetMapping public List<DevUserResponse> list(){ return service.list(); }
  @GetMapping("/{id}") public DevUserResponse get(@PathVariable UUID id){ return service.get(id); }
}
