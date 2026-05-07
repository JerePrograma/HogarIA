package com.hogaria.dto;
import com.hogaria.entity.MoneyTransaction;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;import java.time.LocalDate;import java.util.UUID;
public record TransactionCreateRequest(@NotNull UUID profileId,@NotNull UUID accountId,@NotNull UUID categoryId,@NotNull MoneyTransaction.MovementType movementType,@NotNull LocalDate realDate,@NotNull LocalDate budgetDate,@NotNull @DecimalMin("0.01") BigDecimal amount,@NotBlank @Pattern(regexp="^[A-Z]{3}$") String currency,String description,MoneyTransaction.Origin origin,MoneyTransaction.Status status) {}
