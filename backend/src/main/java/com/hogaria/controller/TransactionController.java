package com.hogaria.controller;

import com.hogaria.dto.TransactionCreateRequest;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.service.TransactionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
  private final TransactionService service;
  public TransactionController(TransactionService service) { this.service = service; }
  @PostMapping
  public MoneyTransaction create(@Valid @RequestBody TransactionCreateRequest request, @RequestHeader("X-User-Id") UUID userId) {
    return service.create(request, userId);
  }
}
