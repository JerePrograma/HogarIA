package com.hogaria.service;

import com.hogaria.dto.MonthlyPlanReconciliationDtos.*;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.entity.MonthlyPlanTransactionMatch;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.MonthlyPlanItemRepository;
import com.hogaria.repository.MonthlyPlanTransactionMatchRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonthlyPlanReconciliationService {

  private static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.20");
  private static final Set<MonthlyPlanItem.Status> NON_EXECUTABLE_STATUSES =
      Set.of(MonthlyPlanItem.Status.CANCELLED);

  private final FinancialProfileRepository profiles;
  private final MonthlyPlanItemRepository items;
  private final MoneyTransactionRepository transactions;
  private final MonthlyPlanTransactionMatchRepository matches;
  private final MonthlyPlanAmountCalculator amountCalculator = new MonthlyPlanAmountCalculator();

  public MonthlyPlanReconciliationService(
      FinancialProfileRepository profiles,
      MonthlyPlanItemRepository items,
      MoneyTransactionRepository transactions,
      MonthlyPlanTransactionMatchRepository matches
  ) {
    this.profiles = profiles;
    this.items = items;
    this.transactions = transactions;
    this.matches = matches;
  }

  @Transactional(readOnly = true)
  public MonthlyPlanReconciliationSummaryResponse monthly(
      UUID userId,
      UUID profileId,
      int year,
      int month
  ) {
    ensureProfile(profileId, userId);

    var periodItems = findItems(profileId, year, month);
    var periodTransactions = findTransactions(profileId, year, month);
    var links = findMatches(profileId, periodItems, periodTransactions);

    return buildSummary(profileId, year, month, periodItems, periodTransactions, links);
  }

  @Transactional
  public TransactionMatchResponse confirm(
      UUID userId,
      UUID profileId,
      ConfirmPlanTransactionMatchRequest request
  ) {
    ensureProfile(profileId, userId);

    var item = items.findByIdAndProfileId(request.itemId(), profileId)
        .orElseThrow(() -> new NotFoundException("Item de planificación no encontrado"));

    var tx = transactions.findByIdAndProfileId(request.transactionId(), profileId)
        .orElseThrow(() -> new NotFoundException("Movimiento no encontrado"));

    validateMatchable(item, tx, request.matchedAmount());

    var existing = matches.findByProfileIdAndMonthlyPlanItemIdAndMoneyTransactionId(
        profileId,
        item.getId(),
        tx.getId()
    );

    var match = existing.orElseGet(MonthlyPlanTransactionMatch::new);
    match.setProfileId(profileId);
    match.setMonthlyPlanItemId(item.getId());
    match.setMoneyTransactionId(tx.getId());
    match.setMatchedAmount(request.matchedAmount());
    match.setMatchType(request.matchType() == null
        ? MonthlyPlanTransactionMatch.MatchType.MANUAL
        : request.matchType());
    match.setConfidence(request.confidence() == null
        ? MonthlyPlanTransactionMatch.MatchConfidence.HIGH
        : request.confidence());
    match.setNote(request.note());

    var saved = matches.save(match);
    synchronizeLegacyTransactionId(item, tx);
    updateItemStatusFromMatches(item, List.of(saved));

    return toMatchResponse(saved);
  }

  @Transactional
  public void delete(UUID userId, UUID profileId, UUID matchId) {
    ensureProfile(profileId, userId);

    var match = matches.findByIdAndProfileId(matchId, profileId)
        .orElseThrow(() -> new NotFoundException("Vínculo de conciliación no encontrado"));

    var item = items.findByIdAndProfileId(match.getMonthlyPlanItemId(), profileId).orElse(null);
    matches.delete(match);

    if (item != null) {
      var remainingLinks = matches.findByProfileIdAndMonthlyPlanItemIdIn(profileId, List.of(item.getId()));
      updateItemStatusFromMatches(item, remainingLinks);
    }
  }

  private MonthlyPlanReconciliationSummaryResponse buildSummary(
      UUID profileId,
      int year,
      int month,
      List<MonthlyPlanItem> periodItems,
      List<MoneyTransaction> periodTransactions,
      List<MonthlyPlanTransactionMatch> links
  ) {
    var matchesByItem = links.stream()
        .collect(Collectors.groupingBy(MonthlyPlanTransactionMatch::getMonthlyPlanItemId));

    var matchedTransactionIds = links.stream()
        .map(MonthlyPlanTransactionMatch::getMoneyTransactionId)
        .collect(Collectors.toSet());

    var plannedResponses = periodItems.stream()
        .map(item -> toItemResponse(item, matchesByItem.getOrDefault(item.getId(), List.of())))
        .toList();

    var unplannedTransactions = periodTransactions.stream()
        .filter(tx -> !matchedTransactionIds.contains(tx.getId()))
        .filter(tx -> tx.getStatus() != MoneyTransaction.Status.IGNORED)
        .map(this::toUnplannedResponse)
        .toList();

    var suggestedMatches = suggest(periodItems, periodTransactions, links);

    var plannedTotal = plannedResponses.stream()
        .map(PlanItemReconciliationResponse::plannedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    var matchedTotal = plannedResponses.stream()
        .map(PlanItemReconciliationResponse::matchedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    var remainingTotal = plannedResponses.stream()
        .map(PlanItemReconciliationResponse::remainingAmount)
        .filter(value -> value.signum() > 0)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    var unplannedTotal = unplannedTransactions.stream()
        .map(UnplannedTransactionResponse::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new MonthlyPlanReconciliationSummaryResponse(
        profileId,
        year,
        month,
        plannedTotal,
        matchedTotal,
        remainingTotal,
        unplannedTotal,
        plannedResponses.size(),
        (int) plannedResponses.stream().filter(p -> p.matchedAmount().signum() > 0).count(),
        (int) plannedResponses.stream().filter(p -> p.executionStatus() == PlanExecutionStatus.PARTIALLY_EXECUTED).count(),
        (int) plannedResponses.stream().filter(p -> p.executionStatus() == PlanExecutionStatus.OVER_EXECUTED).count(),
        unplannedTransactions.size(),
        plannedResponses,
        unplannedTransactions,
        suggestedMatches
    );
  }

  private PlanItemReconciliationResponse toItemResponse(
      MonthlyPlanItem item,
      List<MonthlyPlanTransactionMatch> itemMatches
  ) {
    var plannedAmount = amountCalculator.targetAmountForReconciliation(item);
    var matchedAmount = itemMatches.stream()
        .map(MonthlyPlanTransactionMatch::getMatchedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    var remainingAmount = plannedAmount.subtract(matchedAmount);
    var status = executionStatus(item, plannedAmount, matchedAmount);

    return new PlanItemReconciliationResponse(
        item.getId(),
        item.getTitle(),
        item.getType(),
        item.getStatus(),
        item.getCategoryId(),
        item.getAccountId(),
        item.getExpectedDate(),
        plannedAmount,
        matchedAmount,
        remainingAmount,
        status,
        itemMatches.stream().map(this::toMatchResponse).toList()
    );
  }

  private PlanExecutionStatus executionStatus(
      MonthlyPlanItem item,
      BigDecimal plannedAmount,
      BigDecimal matchedAmount
  ) {
    if (item.getStatus() == MonthlyPlanItem.Status.CANCELLED) {
      return PlanExecutionStatus.CANCELLED;
    }

    if (item.getType() == MonthlyPlanItem.Type.TODO) {
      return PlanExecutionStatus.NOT_APPLICABLE;
    }

    if (plannedAmount.signum() == 0) {
      return PlanExecutionStatus.UNPRICED;
    }

    if (matchedAmount.signum() == 0) {
      return PlanExecutionStatus.NOT_EXECUTED;
    }

    var comparison = matchedAmount.compareTo(plannedAmount);

    if (comparison < 0) {
      return PlanExecutionStatus.PARTIALLY_EXECUTED;
    }

    if (comparison == 0) {
      return PlanExecutionStatus.EXECUTED;
    }

    return PlanExecutionStatus.OVER_EXECUTED;
  }

  private List<SuggestedPlanTransactionMatchResponse> suggest(
      List<MonthlyPlanItem> periodItems,
      List<MoneyTransaction> periodTransactions,
      List<MonthlyPlanTransactionMatch> links
  ) {
    var linkedPairs = links.stream()
        .map(link -> link.getMonthlyPlanItemId() + ":" + link.getMoneyTransactionId())
        .collect(Collectors.toSet());

    var usedTransactionIds = links.stream()
        .map(MonthlyPlanTransactionMatch::getMoneyTransactionId)
        .collect(Collectors.toSet());

    var suggestions = new ArrayList<SuggestedPlanTransactionMatchResponse>();

    for (var item : periodItems) {
      if (!isPlanItemMatchCandidate(item)) {
        continue;
      }

      for (var tx : periodTransactions) {
        if (!isTransactionMatchCandidate(tx)) {
          continue;
        }

        var pairKey = item.getId() + ":" + tx.getId();

        if (linkedPairs.contains(pairKey) || usedTransactionIds.contains(tx.getId())) {
          continue;
        }

        var score = score(item, tx);

        if (score.score() < 45) {
          continue;
        }

        suggestions.add(new SuggestedPlanTransactionMatchResponse(
            item.getId(),
            tx.getId(),
            amountCalculator.targetAmountForReconciliation(item).min(tx.getAmount()),
            confidence(score.score()),
            score.score(),
            score.reasons()
        ));
      }
    }

    return suggestions.stream()
        .sorted(Comparator.comparing(SuggestedPlanTransactionMatchResponse::score).reversed())
        .limit(20)
        .toList();
  }

  private MatchScore score(MonthlyPlanItem item, MoneyTransaction tx) {
    int score = 0;
    var reasons = new ArrayList<String>();

    if (compatible(item.getType(), tx.getMovementType())) {
      score += 30;
      reasons.add("Tipo compatible.");
    }

    if (item.getCategoryId() != null && item.getCategoryId().equals(tx.getCategoryId())) {
      score += 25;
      reasons.add("Misma categoría.");
    }

    if (item.getAccountId() != null && item.getAccountId().equals(tx.getAccountId())) {
      score += 15;
      reasons.add("Misma cuenta.");
    }

    if (isSimilarAmount(amountCalculator.targetAmountForReconciliation(item), tx.getAmount())) {
      score += 20;
      reasons.add("Monto similar.");
    }

    if (item.getExpectedDate() != null && Math.abs(item.getExpectedDate().toEpochDay() - tx.getBudgetDate().toEpochDay()) <= 3) {
      score += 10;
      reasons.add("Fecha cercana.");
    }

    if (sharedTokens(item.getTitle(), tx.getDescription()) >= 2) {
      score += 15;
      reasons.add("Descripción similar.");
    }

    return new MatchScore(score, reasons);
  }

  private void validateMatchable(MonthlyPlanItem item, MoneyTransaction tx, BigDecimal matchedAmount) {
    if (!isPlanItemMatchCandidate(item)) {
      throw new BadRequestException("El ítem no admite conciliación");
    }

    if (!isTransactionMatchCandidate(tx)) {
      throw new BadRequestException("El movimiento no admite conciliación");
    }

    if (!compatible(item.getType(), tx.getMovementType())) {
      throw new BadRequestException("Tipo de movimiento incompatible con el ítem planificado");
    }

    if (matchedAmount == null || matchedAmount.signum() <= 0) {
      throw new BadRequestException("El monto conciliado debe ser positivo");
    }

    if (matchedAmount.compareTo(tx.getAmount()) > 0) {
      throw new BadRequestException("El monto conciliado no puede superar el movimiento");
    }
  }

  private void updateItemStatusFromMatches(
      MonthlyPlanItem item,
      List<MonthlyPlanTransactionMatch> itemMatches
  ) {
    if (!isPlanItemMatchCandidate(item)) {
      return;
    }

    var plannedAmount = amountCalculator.targetAmountForReconciliation(item);
    var matchedAmount = itemMatches.stream()
        .map(MonthlyPlanTransactionMatch::getMatchedAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (plannedAmount.signum() > 0 && matchedAmount.compareTo(plannedAmount) >= 0) {
      item.setStatus(isIncomeLike(item.getType())
          ? MonthlyPlanItem.Status.COLLECTED
          : MonthlyPlanItem.Status.PAID);
      items.save(item);
    }
  }

  private void synchronizeLegacyTransactionId(MonthlyPlanItem item, MoneyTransaction tx) {
    if (item.getTransactionId() == null) {
      item.setTransactionId(tx.getId());
      items.save(item);
    }
  }

  private List<MonthlyPlanItem> findItems(UUID profileId, int year, int month) {
    return items.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, year, month).stream()
        .sorted(Comparator
            .comparing(MonthlyPlanItem::getExpectedDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(MonthlyPlanItem::getPriority)
            .thenComparing(MonthlyPlanItem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
        .toList();
  }

  private List<MoneyTransaction> findTransactions(UUID profileId, int year, int month) {
    var from = LocalDate.of(year, month, 1);
    var to = from.withDayOfMonth(from.lengthOfMonth());
    return transactions.findByProfileIdAndBudgetDateBetween(profileId, from, to);
  }

  private List<MonthlyPlanTransactionMatch> findMatches(
      UUID profileId,
      List<MonthlyPlanItem> periodItems,
      List<MoneyTransaction> periodTransactions
  ) {
    var itemIds = ids(periodItems.stream().map(MonthlyPlanItem::getId).toList());
    var txIds = ids(periodTransactions.stream().map(MoneyTransaction::getId).toList());
    var out = new ArrayList<MonthlyPlanTransactionMatch>();

    if (!itemIds.isEmpty()) {
      out.addAll(matches.findByProfileIdAndMonthlyPlanItemIdIn(profileId, itemIds));
    }

    if (!txIds.isEmpty()) {
      out.addAll(matches.findByProfileIdAndMoneyTransactionIdIn(profileId, txIds));
    }

    return out.stream()
        .collect(Collectors.toMap(MonthlyPlanTransactionMatch::getId, link -> link, (a, b) -> a))
        .values()
        .stream()
        .toList();
  }

  private Collection<UUID> ids(Collection<UUID> values) {
    return values.stream().filter(Objects::nonNull).collect(Collectors.toSet());
  }

  private boolean isPlanItemMatchCandidate(MonthlyPlanItem item) {
    return item.getType() != MonthlyPlanItem.Type.TODO
        && !NON_EXECUTABLE_STATUSES.contains(item.getStatus());
  }

  private boolean isTransactionMatchCandidate(MoneyTransaction tx) {
    return tx.getStatus() != MoneyTransaction.Status.IGNORED;
  }

  private boolean compatible(MonthlyPlanItem.Type type, MoneyTransaction.MovementType movementType) {
    if (type == null || movementType == null) {
      return false;
    }

    return switch (type) {
      case INCOME, RECOVERY -> movementType == MoneyTransaction.MovementType.INCOME;
      case EXPENSE, DEBT -> movementType == MoneyTransaction.MovementType.EXPENSE;
      case SAVING -> movementType == MoneyTransaction.MovementType.SAVING;
      case TRANSFER -> movementType == MoneyTransaction.MovementType.TRANSFER;
      case TODO -> false;
    };
  }

  private boolean isIncomeLike(MonthlyPlanItem.Type type) {
    return type == MonthlyPlanItem.Type.INCOME || type == MonthlyPlanItem.Type.RECOVERY;
  }

  private boolean isSimilarAmount(BigDecimal planned, BigDecimal actual) {
    if (planned == null || actual == null || planned.signum() <= 0 || actual.signum() <= 0) {
      return false;
    }

    var delta = planned.subtract(actual).abs();
    var denominator = planned.max(actual);

    if (denominator.signum() == 0) {
      return false;
    }

    return delta.divide(denominator, 4, RoundingMode.HALF_UP).compareTo(AMOUNT_TOLERANCE) <= 0;
  }

  private int sharedTokens(String a, String b) {
    var left = tokens(a);
    var right = tokens(b);
    left.retainAll(right);
    return left.size();
  }

  private Set<String> tokens(String value) {
    var normalized = normalize(value);
    var out = new HashSet<String>();

    for (var token : normalized.split(" ")) {
      if (token.length() > 2 && !token.chars().allMatch(Character::isDigit)) {
        out.add(token);
      }
    }

    return out;
  }

  private String normalize(String value) {
    if (value == null) {
      return "";
    }

    return Pattern.compile("[^\\p{Alnum}\\s]")
        .matcher(Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", ""))
        .replaceAll(" ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private SuggestedMatchConfidence confidence(int score) {
    if (score >= 80) {
      return SuggestedMatchConfidence.HIGH;
    }

    if (score >= 60) {
      return SuggestedMatchConfidence.MEDIUM;
    }

    return SuggestedMatchConfidence.LOW;
  }

  private TransactionMatchResponse toMatchResponse(MonthlyPlanTransactionMatch match) {
    return new TransactionMatchResponse(
        match.getId(),
        match.getMoneyTransactionId(),
        match.getMatchedAmount(),
        match.getMatchType(),
        match.getConfidence(),
        match.getNote()
    );
  }

  private UnplannedTransactionResponse toUnplannedResponse(MoneyTransaction tx) {
    return new UnplannedTransactionResponse(
        tx.getId(),
        tx.getAccountId(),
        tx.getCategoryId(),
        tx.getMovementType(),
        tx.getRealDate(),
        tx.getBudgetDate(),
        tx.getAmount(),
        tx.getCurrency(),
        tx.getDescription(),
        tx.getOrigin(),
        tx.getStatus()
    );
  }

  private void ensureProfile(UUID profileId, UUID userId) {
    profiles.findByIdAndUserId(profileId, userId)
        .orElseThrow(() -> new ForbiddenException("El perfil no pertenece al usuario actual."));
  }

  private record MatchScore(int score, List<String> reasons) {}
}
