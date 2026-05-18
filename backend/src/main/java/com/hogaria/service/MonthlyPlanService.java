package com.hogaria.service;

import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemCreateRequest;
import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemResponse;
import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanItemUpdateRequest;
import com.hogaria.dto.MonthlyPlanDtos.MonthlyPlanSummaryResponse;
import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.MonthlyPlanItemRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonthlyPlanService {

  private static final Set<MonthlyPlanItem.Status> PENDING_STATUSES = Set.of(
      MonthlyPlanItem.Status.DRAFT,
      MonthlyPlanItem.Status.ESTIMATED,
      MonthlyPlanItem.Status.SCHEDULED,
      MonthlyPlanItem.Status.DUE
  );

  private final MonthlyPlanItemRepository repo;
  private final FinancialProfileRepository profiles;
  private final CategoryRepository categories;
  private final AccountRepository accounts;
  private final MoneyTransactionRepository txRepo;
  private final MonthlyPlanAmountCalculator amountCalculator = new MonthlyPlanAmountCalculator();

  public MonthlyPlanService(
      MonthlyPlanItemRepository repo,
      FinancialProfileRepository profiles,
      CategoryRepository categories,
      AccountRepository accounts,
      MoneyTransactionRepository txRepo
  ) {
    this.repo = repo;
    this.profiles = profiles;
    this.categories = categories;
    this.accounts = accounts;
    this.txRepo = txRepo;
  }

  @Transactional
  public MonthlyPlanItemResponse create(
      UUID userId,
      UUID profileId,
      MonthlyPlanItemCreateRequest request
  ) {
    ensureProfile(profileId, userId);

    var item = MonthlyPlanItem.builder()
        .profileId(profileId)
        .categoryId(request.categoryId())
        .accountId(request.accountId())
        .type(request.type())
        .title(request.title())
        .description(request.description())
        .expectedDate(request.expectedDate())
        .periodYear(request.periodYear())
        .periodMonth(request.periodMonth())
        .amount(request.amount())
        .minAmount(request.minAmount())
        .maxAmount(request.maxAmount())
        .currency(request.currency() == null ? "ARS" : request.currency())
        .expectedRecoveryAmount(request.expectedRecoveryAmount())
        .expectedRecoveryPercent(request.expectedRecoveryPercent())
        .counterparty(request.counterparty())
        .installmentNumber(request.installmentNumber())
        .installmentTotal(request.installmentTotal())
        .priority(request.priority() == null ? MonthlyPlanItem.Priority.IMPORTANT : request.priority())
        .status(request.status() == null ? MonthlyPlanItem.Status.ESTIMATED : request.status())
        .source(request.source() == null ? MonthlyPlanItem.Source.MANUAL : request.source())
        .build();

    validate(item);
    validateRefs(profileId, item.getAccountId(), item.getCategoryId());

    return toResponse(repo.save(item));
  }

  @Transactional(readOnly = true)
  public MonthlyPlanSummaryResponse summary(UUID userId, UUID profileId, int year, int month) {
    ensureProfile(profileId, userId);

    var items = repo.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, year, month).stream()
        .sorted(Comparator
            .comparing(MonthlyPlanItem::getExpectedDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(MonthlyPlanItem::getPriority)
            .thenComparing(MonthlyPlanItem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();

    var totals = new MonthlyPlanTotals();
    var today = LocalDate.now();

    for (var item : items) {
      totals.include(item, amountCalculator.calculate(item), today);
    }

    return new MonthlyPlanSummaryResponse(
        totals.totalIncomeMin,
        totals.totalIncomeMax,
        totals.totalExpenseMin,
        totals.totalExpenseMax,
        totals.totalRecoveryMin,
        totals.totalRecoveryMax,
        totals.netMin,
        totals.netMax,
        totals.pendingIncome,
        totals.pendingExpense,
        totals.unpricedCount,
        totals.dueNext7DaysCount,
        items.stream().map(this::toResponse).toList()
    );
  }

  @Transactional
  public MonthlyPlanItemResponse update(
      UUID userId,
      UUID profileId,
      UUID itemId,
      MonthlyPlanItemUpdateRequest request
  ) {
    ensureProfile(profileId, userId);

    var item = repo.findByIdAndProfileId(itemId, profileId)
        .orElseThrow(() -> new NotFoundException("Item no encontrado"));

    applyClears(item, request);
    applyChanges(item, request);
    validate(item);
    validateRefs(profileId, item.getAccountId(), item.getCategoryId());

    return toResponse(repo.save(item));
  }

  @Transactional
  public void delete(UUID userId, UUID profileId, UUID itemId) {
    ensureProfile(profileId, userId);
    repo.delete(repo.findByIdAndProfileId(itemId, profileId)
        .orElseThrow(() -> new NotFoundException("Item no encontrado")));
  }

  @Transactional
  public MonthlyPlanItemResponse convert(UUID userId, UUID profileId, UUID itemId) {
    ensureProfile(profileId, userId);

    var item = repo.findByIdAndProfileId(itemId, profileId)
        .orElseThrow(() -> new NotFoundException("Item no encontrado"));

    validateConvertible(item);

    var amount = amountCalculator.exactConvertibleAmount(item)
        .orElseThrow(() -> new BadRequestException("Falta cuenta/categoría/monto exacto"));

    if (item.getAccountId() == null || item.getCategoryId() == null) {
      throw new BadRequestException("Falta cuenta/categoría/monto exacto");
    }

    var transaction = txRepo.save(MoneyTransaction.builder()
        .profileId(profileId)
        .accountId(item.getAccountId())
        .categoryId(item.getCategoryId())
        .movementType(toMovementType(item.getType()))
        .realDate(item.getExpectedDate() == null ? LocalDate.now() : item.getExpectedDate())
        .budgetDate(LocalDate.of(item.getPeriodYear(), item.getPeriodMonth(), 1))
        .amount(amount)
        .currency(item.getCurrency())
        .description(item.getTitle())
        .origin(MoneyTransaction.Origin.SYSTEM)
        .status(convertedTransactionStatus(item.getStatus()))
        .build());

    item.setTransactionId(transaction.getId());
    item.setStatus(convertedItemStatus(item.getType()));

    return toResponse(repo.save(item));
  }

  private void validate(MonthlyPlanItem item) {
    if (item.getTitle() == null || item.getTitle().trim().isEmpty()) {
      throw new BadRequestException("Título requerido");
    }

    item.setTitle(item.getTitle().trim());

    if (item.getPeriodMonth() == null || item.getPeriodMonth() < 1 || item.getPeriodMonth() > 12) {
      throw new BadRequestException("Mes inválido");
    }

    assertNonNegative(item.getAmount(), "Los montos no pueden ser negativos");
    assertNonNegative(item.getMinAmount(), "Los montos no pueden ser negativos");
    assertNonNegative(item.getMaxAmount(), "Los montos no pueden ser negativos");
    assertNonNegative(item.getExpectedRecoveryAmount(), "Los montos no pueden ser negativos");

    if (item.getMinAmount() != null
        && item.getMaxAmount() != null
        && item.getMinAmount().compareTo(item.getMaxAmount()) > 0) {
      throw new BadRequestException("Mínimo no puede superar máximo");
    }

    if (item.getExpectedRecoveryPercent() != null
        && (item.getExpectedRecoveryPercent().signum() < 0
            || item.getExpectedRecoveryPercent().compareTo(new BigDecimal("100")) > 0)) {
      throw new BadRequestException("Recupero % inválido");
    }

    if (item.getInstallmentNumber() != null && item.getInstallmentNumber() <= 0) {
      throw new BadRequestException("Número de cuota inválido");
    }

    if (item.getInstallmentTotal() != null && item.getInstallmentTotal() <= 0) {
      throw new BadRequestException("Total de cuotas inválido");
    }

    if (item.getInstallmentNumber() != null
        && item.getInstallmentTotal() != null
        && item.getInstallmentNumber() > item.getInstallmentTotal()) {
      throw new BadRequestException("Cuota inválida");
    }

    if (item.getCurrency() == null || !item.getCurrency().trim().matches("[A-Za-z]{3}")) {
      throw new BadRequestException("Moneda inválida");
    }

    item.setCurrency(item.getCurrency().trim().toUpperCase());

    var amounts = amountCalculator.calculate(item);

    if (item.getExpectedRecoveryAmount() != null
        && item.getExpectedRecoveryAmount().compareTo(amounts.grossMax()) > 0) {
      throw new BadRequestException("Recupero no puede superar bruto");
    }
  }

  private void validateRefs(UUID profileId, UUID accountId, UUID categoryId) {
    if (accountId != null && !accounts.existsByIdAndProfileId(accountId, profileId)) {
      throw new BadRequestException("Cuenta inválida para perfil");
    }

    if (categoryId == null) {
      return;
    }

    var category = categories.findById(categoryId)
        .orElseThrow(() -> new NotFoundException("Categoría no encontrada"));

    if (category.getProfileId() != null && !Objects.equals(category.getProfileId(), profileId)) {
      throw new ForbiddenException("Categoría inválida para perfil");
    }
  }

  private void validateConvertible(MonthlyPlanItem item) {
    if (item.getTransactionId() != null) {
      throw new BadRequestException("Item ya convertido");
    }

    if (item.getStatus() == MonthlyPlanItem.Status.CANCELLED) {
      throw new BadRequestException("Item cancelado");
    }

    if (item.getType() == MonthlyPlanItem.Type.TODO) {
      throw new BadRequestException("Tipo no convertible");
    }
  }

  private MoneyTransaction.MovementType toMovementType(MonthlyPlanItem.Type type) {
    return switch (type) {
      case INCOME, RECOVERY -> MoneyTransaction.MovementType.INCOME;
      case EXPENSE, DEBT -> MoneyTransaction.MovementType.EXPENSE;
      case SAVING -> MoneyTransaction.MovementType.SAVING;
      case TRANSFER -> MoneyTransaction.MovementType.TRANSFER;
      case TODO -> throw new BadRequestException("Tipo no convertible");
    };
  }

  private MoneyTransaction.Status convertedTransactionStatus(MonthlyPlanItem.Status status) {
    return status == MonthlyPlanItem.Status.PAID || status == MonthlyPlanItem.Status.COLLECTED
        ? MoneyTransaction.Status.CONFIRMED
        : MoneyTransaction.Status.PENDING;
  }

  private MonthlyPlanItem.Status convertedItemStatus(MonthlyPlanItem.Type type) {
    return type == MonthlyPlanItem.Type.INCOME || type == MonthlyPlanItem.Type.RECOVERY
        ? MonthlyPlanItem.Status.COLLECTED
        : MonthlyPlanItem.Status.PAID;
  }

  private void applyClears(MonthlyPlanItem item, MonthlyPlanItemUpdateRequest request) {
    if (Boolean.TRUE.equals(request.clearExpectedDate())) item.setExpectedDate(null);
    if (Boolean.TRUE.equals(request.clearAmount())) item.setAmount(null);

    if (Boolean.TRUE.equals(request.clearRange())) {
      item.setMinAmount(null);
      item.setMaxAmount(null);
    }

    if (Boolean.TRUE.equals(request.clearRecovery())) {
      item.setExpectedRecoveryAmount(null);
      item.setExpectedRecoveryPercent(null);
    }

    if (Boolean.TRUE.equals(request.clearCounterparty())) item.setCounterparty(null);

    if (Boolean.TRUE.equals(request.clearInstallment())) {
      item.setInstallmentNumber(null);
      item.setInstallmentTotal(null);
    }

    if (Boolean.TRUE.equals(request.clearCategory())) item.setCategoryId(null);
    if (Boolean.TRUE.equals(request.clearAccount())) item.setAccountId(null);
  }

  private void applyChanges(MonthlyPlanItem item, MonthlyPlanItemUpdateRequest request) {
    if (request.type() != null) item.setType(request.type());
    if (request.title() != null) item.setTitle(request.title());
    if (request.description() != null) item.setDescription(request.description());
    if (request.expectedDate() != null) item.setExpectedDate(request.expectedDate());
    if (request.periodYear() != null) item.setPeriodYear(request.periodYear());
    if (request.periodMonth() != null) item.setPeriodMonth(request.periodMonth());
    if (request.amount() != null) item.setAmount(request.amount());
    if (request.minAmount() != null) item.setMinAmount(request.minAmount());
    if (request.maxAmount() != null) item.setMaxAmount(request.maxAmount());
    if (request.currency() != null) item.setCurrency(request.currency());
    if (request.expectedRecoveryAmount() != null) item.setExpectedRecoveryAmount(request.expectedRecoveryAmount());
    if (request.expectedRecoveryPercent() != null) item.setExpectedRecoveryPercent(request.expectedRecoveryPercent());
    if (request.counterparty() != null) item.setCounterparty(request.counterparty());
    if (request.installmentNumber() != null) item.setInstallmentNumber(request.installmentNumber());
    if (request.installmentTotal() != null) item.setInstallmentTotal(request.installmentTotal());
    if (request.priority() != null) item.setPriority(request.priority());
    if (request.status() != null) item.setStatus(request.status());
    if (request.source() != null) item.setSource(request.source());
    if (request.categoryId() != null) item.setCategoryId(request.categoryId());
    if (request.accountId() != null) item.setAccountId(request.accountId());
  }

  private MonthlyPlanItemResponse toResponse(MonthlyPlanItem item) {
    var amounts = amountCalculator.calculate(item);

    return new MonthlyPlanItemResponse(
        item.getId(),
        item.getProfileId(),
        item.getCategoryId(),
        item.getAccountId(),
        item.getType(),
        item.getTitle(),
        item.getDescription(),
        item.getExpectedDate(),
        item.getPeriodYear(),
        item.getPeriodMonth(),
        item.getAmount(),
        item.getMinAmount(),
        item.getMaxAmount(),
        item.getCurrency(),
        item.getExpectedRecoveryAmount(),
        item.getExpectedRecoveryPercent(),
        item.getCounterparty(),
        item.getInstallmentNumber(),
        item.getInstallmentTotal(),
        item.getPriority(),
        item.getStatus(),
        item.getSource(),
        item.getTransactionId(),
        amounts.grossMin(),
        amounts.grossMax(),
        amounts.recoveryMin(),
        amounts.recoveryMax(),
        amounts.netMin(),
        amounts.netMax(),
        item.getCreatedAt(),
        item.getUpdatedAt()
    );
  }

  private void ensureProfile(UUID profileId, UUID userId) {
    profiles.findByIdAndUserId(profileId, userId)
        .orElseThrow(() -> new ForbiddenException("Profile does not belong to user"));
  }

  private void assertNonNegative(BigDecimal value, String message) {
    if (value != null && value.signum() < 0) {
      throw new BadRequestException(message);
    }
  }

  private static final class MonthlyPlanTotals {
    private BigDecimal totalIncomeMin = BigDecimal.ZERO;
    private BigDecimal totalIncomeMax = BigDecimal.ZERO;
    private BigDecimal totalExpenseMin = BigDecimal.ZERO;
    private BigDecimal totalExpenseMax = BigDecimal.ZERO;
    private BigDecimal totalRecoveryMin = BigDecimal.ZERO;
    private BigDecimal totalRecoveryMax = BigDecimal.ZERO;
    private BigDecimal netMin = BigDecimal.ZERO;
    private BigDecimal netMax = BigDecimal.ZERO;
    private BigDecimal pendingIncome = BigDecimal.ZERO;
    private BigDecimal pendingExpense = BigDecimal.ZERO;
    private int unpricedCount;
    private int dueNext7DaysCount;

    private void include(
        MonthlyPlanItem item,
        MonthlyPlanAmountCalculator.MonthlyPlanAmounts amounts,
        LocalDate today
    ) {
      var cancelled = item.getStatus() == MonthlyPlanItem.Status.CANCELLED;

      if (item.getAmount() == null && item.getMinAmount() == null && item.getMaxAmount() == null) {
        unpricedCount++;
      }

      if (!cancelled
          && item.getExpectedDate() != null
          && !item.getExpectedDate().isBefore(today)
          && !item.getExpectedDate().isAfter(today.plusDays(7))) {
        dueNext7DaysCount++;
      }

      if (cancelled || item.getType() == MonthlyPlanItem.Type.TODO) {
        return;
      }

      if (item.getType() == MonthlyPlanItem.Type.INCOME || item.getType() == MonthlyPlanItem.Type.RECOVERY) {
        includeIncome(item, amounts);
      } else {
        includeExpense(item, amounts);
      }
    }

    private void includeIncome(
        MonthlyPlanItem item,
        MonthlyPlanAmountCalculator.MonthlyPlanAmounts amounts
    ) {
      totalIncomeMin = totalIncomeMin.add(amounts.grossMin());
      totalIncomeMax = totalIncomeMax.add(amounts.grossMax());
      netMin = netMin.add(amounts.netMin());
      netMax = netMax.add(amounts.netMax());

      if (PENDING_STATUSES.contains(item.getStatus())) {
        pendingIncome = pendingIncome.add(amounts.netMax());
      }
    }

    private void includeExpense(
        MonthlyPlanItem item,
        MonthlyPlanAmountCalculator.MonthlyPlanAmounts amounts
    ) {
      totalExpenseMin = totalExpenseMin.add(amounts.grossMin());
      totalExpenseMax = totalExpenseMax.add(amounts.grossMax());
      totalRecoveryMin = totalRecoveryMin.add(amounts.recoveryMin());
      totalRecoveryMax = totalRecoveryMax.add(amounts.recoveryMax());
      netMin = netMin.subtract(amounts.netMax());
      netMax = netMax.subtract(amounts.netMin());

      if (PENDING_STATUSES.contains(item.getStatus())) {
        pendingExpense = pendingExpense.add(amounts.netMax());
      }
    }
  }
}
