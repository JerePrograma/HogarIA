package com.hogaria.service;

import com.hogaria.dto.TransactionCreateRequest;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.repository.MoneyTransactionRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {
  private final MoneyTransactionRepository repository;
  public TransactionService(MoneyTransactionRepository repository) { this.repository = repository; }
  public MoneyTransaction create(TransactionCreateRequest request, UUID userId) {
    MoneyTransaction tx = new MoneyTransaction();
    return repository.save(tx);
  }
}
