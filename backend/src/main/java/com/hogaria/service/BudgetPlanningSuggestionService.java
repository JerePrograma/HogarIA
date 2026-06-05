package com.hogaria.service;

import com.hogaria.dto.BudgetPlanningSuggestionDtos.ApplyBudgetSuggestion;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.ApplyMonthlyPlanSuggestion;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.BudgetPlanningSuggestionCommitRequest;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.BudgetPlanningSuggestionCommitResponse;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.BudgetPlanningSuggestionPreviewRequest;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.BudgetPlanningSuggestionPreviewResponse;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.BudgetPlanningSuggestionTotals;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.BudgetSuggestion;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.MonthlyPlanSuggestion;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.SuggestionConfidence;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.SuggestionMode;
import com.hogaria.dto.BudgetPlanningSuggestionDtos.SuggestionTarget;
import com.hogaria.entity.Account;
import com.hogaria.entity.BudgetCategoryItem;
import com.hogaria.entity.BudgetMonth;
import com.hogaria.entity.BudgetYear;
import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.entity.MonthlyPlanItem;
import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.repository.AccountRepository;
import com.hogaria.repository.BudgetCategoryItemRepository;
import com.hogaria.repository.BudgetMonthRepository;
import com.hogaria.repository.BudgetYearRepository;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import com.hogaria.repository.MonthlyPlanItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetPlanningSuggestionService {

    private static final BigDecimal DEFAULT_ROUNDING_MULTIPLE = new BigDecimal("1000");
    private static final BigDecimal OUTLIER_MULTIPLIER = new BigDecimal("3");
    private static final BigDecimal AMOUNT_SIMILARITY_PERCENT = new BigDecimal("0.10");
    private static final BigDecimal MIN_SIMILARITY_TOLERANCE = new BigDecimal("100");
    private static final Pattern NON_ALNUM = Pattern.compile("[^\\p{Alnum}\\s]");

    private static final Set<String> EXCLUDED_CLASSIFICATION_REASONS = Set.of(
            "INTERNAL_TRANSFER_MATCHED",
            "USER_MARKED_INTERNAL_TRANSFER",
            "USER_IGNORED_CROSS_SOURCE"
    );

    private static final Set<String> CJ_EXCLUDED_REASONS = Set.of(
            "CJPRESTAMOS_DISBURSEMENT",
            "RECOVERABLE_OUTFLOW",
            "CJPRESTAMOS_RECOVERABLE_OUTFLOW",
            "CJPRESTAMOS_PAYMENT_PRINCIPAL_RECOVERY",
            "CJPRESTAMOS_PAYMENT_INTEREST_INCOME"
    );

    private static final Set<MonthlyPlanItem.Type> SUGGESTION_ALLOWED_PLAN_TYPES = Set.of(
            MonthlyPlanItem.Type.INCOME,
            MonthlyPlanItem.Type.EXPENSE,
            MonthlyPlanItem.Type.SAVING,
            MonthlyPlanItem.Type.DEBT,
            MonthlyPlanItem.Type.RECOVERY
    );

    private static final Set<String> GENERIC_DUPLICATE_TOKENS = Set.of(
            "pago",
            "cuota",
            "mensual",
            "debito",
            "credito",
            "tarjeta",
            "compra"
    );

    private static final Set<String> COMMITMENT_KEYWORDS = Set.of(
            "alquiler",
            "expensas",
            "suscripcion",
            "servicio",
            "internet",
            "obra social",
            "prepaga",
            "salud",
            "colegio",
            "universidad",
            "cuota",
            "prestamo",
            "credito",
            "tarjeta",
            "visa",
            "mastercard",
            "mercado credito",
            "mercadocredito",
            "netflix",
            "spotify",
            "osde",
            "swiss"
    );

    private final FinancialProfileRepository profileRepository;
    private final MoneyTransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AccountRepository accountRepository;
    private final BudgetYearRepository budgetYearRepository;
    private final BudgetMonthRepository budgetMonthRepository;
    private final BudgetCategoryItemRepository budgetItemRepository;
    private final MonthlyPlanItemRepository monthlyPlanItemRepository;
    private final MonthlyPlanItemValidator monthlyPlanItemValidator;

    public BudgetPlanningSuggestionService(
            FinancialProfileRepository profileRepository,
            MoneyTransactionRepository transactionRepository,
            CategoryRepository categoryRepository,
            AccountRepository accountRepository,
            BudgetYearRepository budgetYearRepository,
            BudgetMonthRepository budgetMonthRepository,
            BudgetCategoryItemRepository budgetItemRepository,
            MonthlyPlanItemRepository monthlyPlanItemRepository
    ) {
        this.profileRepository = profileRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
        this.budgetYearRepository = budgetYearRepository;
        this.budgetMonthRepository = budgetMonthRepository;
        this.budgetItemRepository = budgetItemRepository;
        this.monthlyPlanItemRepository = monthlyPlanItemRepository;
        this.monthlyPlanItemValidator = new MonthlyPlanItemValidator(categoryRepository, accountRepository);
    }

    @Transactional(readOnly = true)
    public BudgetPlanningSuggestionPreviewResponse preview(
            UUID userId,
            UUID profileId,
            BudgetPlanningSuggestionPreviewRequest request
    ) {
        ensureProfile(profileId, userId);
        validatePeriod(request.year(), request.month());

        SuggestionWindow window = windowFor(request.year(), request.month(), request.mode());
        List<MoneyTransaction> transactions = loadTransactions(profileId, request, window);
        Set<UUID> selectedIds = selectedIds(request.selectedTransactionIds());
        Map<UUID, Category> categoriesById = loadCategories(transactions);
        Map<UUID, Account> accountsById = loadAccounts(transactions);

        List<String> warnings = new ArrayList<>();
        List<BudgetSuggestion> budgetSuggestions = List.of();
        List<MonthlyPlanSuggestion> monthlyPlanSuggestions = List.of();

        if (request.target() == SuggestionTarget.BUDGET || request.target() == SuggestionTarget.BOTH) {
            budgetSuggestions = buildBudgetSuggestions(
                    transactions,
                    categoriesById,
                    selectedIds,
                    request,
                    window
            );
        }

        if (request.target() == SuggestionTarget.MONTHLY_PLAN || request.target() == SuggestionTarget.BOTH) {
            monthlyPlanSuggestions = buildMonthlyPlanSuggestions(
                    profileId,
                    transactions,
                    categoriesById,
                    accountsById,
                    selectedIds,
                    request,
                    window,
                    warnings
            );
        }

        BudgetPlanningSuggestionTotals totals = buildTotals(budgetSuggestions, monthlyPlanSuggestions);

        if (budgetSuggestions.isEmpty() && monthlyPlanSuggestions.isEmpty()) {
            warnings.add("No se encontraron movimientos confirmados y clasificables para generar sugerencias.");
        }

        return new BudgetPlanningSuggestionPreviewResponse(
                budgetSuggestions,
                monthlyPlanSuggestions,
                warnings,
                totals
        );
    }

    @Transactional
    public BudgetPlanningSuggestionCommitResponse commit(
            UUID userId,
            UUID profileId,
            BudgetPlanningSuggestionCommitRequest request
    ) {
        ensureProfile(profileId, userId);
        validatePeriod(request.year(), request.month());

        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int createdBudgetItems = 0;
        int updatedBudgetItems = 0;
        int createdMonthlyPlanItems = 0;
        int skippedDuplicates = 0;

        List<ApplyBudgetSuggestion> budgetItems = request.applyBudgetSuggestions() == null
                ? List.of()
                : request.applyBudgetSuggestions();

        List<ApplyBudgetSuggestion> selectedBudgetItems = budgetItems.stream()
                .filter(Objects::nonNull)
                .filter(item -> !Boolean.FALSE.equals(item.apply()))
                .toList();

        List<ValidatedBudgetSuggestion> validBudgetItems = new ArrayList<>();

        for (ApplyBudgetSuggestion suggestion : selectedBudgetItems) {
            try {
                validBudgetItems.add(validateBudgetSuggestion(profileId, suggestion));
            } catch (RuntimeException ex) {
                errors.add("Presupuesto: " + ex.getMessage());
            }
        }

        if (!validBudgetItems.isEmpty()) {
            BudgetMonth budgetMonth = ensureBudgetMonth(profileId, request.year(), request.month());

            for (ValidatedBudgetSuggestion validated : validBudgetItems) {
                ApplyBudgetSuggestion suggestion = validated.suggestion();
                Category category = validated.category();
                var existing = budgetItemRepository.findByBudgetMonthIdAndCategoryId(
                        budgetMonth.getId(),
                        suggestion.categoryId()
                );

                if (existing.isPresent() && !Boolean.TRUE.equals(request.overwriteExistingBudgetItems())) {
                    skippedDuplicates++;
                    warnings.add("Presupuesto existente no modificado para " + category.getName() + ".");
                    continue;
                }

                BudgetCategoryItem item = existing.orElseGet(() ->
                        BudgetCategoryItem.builder()
                                .budgetMonthId(budgetMonth.getId())
                                .categoryId(suggestion.categoryId())
                                .build()
                );

                item.setBudgetAmount(nonNegative(suggestion.suggestedBudgetAmount()));
                budgetItemRepository.save(item);

                if (existing.isPresent()) {
                    updatedBudgetItems++;
                } else {
                    createdBudgetItems++;
                }
            }
        }

        List<ApplyMonthlyPlanSuggestion> planItems = request.applyMonthlyPlanSuggestions() == null
                ? List.of()
                : request.applyMonthlyPlanSuggestions();

        for (ApplyMonthlyPlanSuggestion suggestion : planItems) {
            if (suggestion == null) {
                continue;
            }

            if (Boolean.FALSE.equals(suggestion.apply())) {
                continue;
            }

            YearMonth operationalPeriod = suggestion.expectedDate() == null
                    ? YearMonth.of(suggestion.periodYear(), suggestion.periodMonth())
                    : YearMonth.from(suggestion.expectedDate());

            MonthlyPlanItem item = MonthlyPlanItem.builder()
                    .profileId(profileId)
                    .categoryId(suggestion.categoryId())
                    .accountId(suggestion.accountId())
                    .type(suggestion.type())
                    .title(suggestion.title())
                    .description(suggestion.description())
                    .expectedDate(suggestion.expectedDate())
                    .periodYear(operationalPeriod.getYear())
                    .periodMonth(operationalPeriod.getMonthValue())
                    .amount(suggestion.amount())
                    .minAmount(suggestion.minAmount())
                    .maxAmount(suggestion.maxAmount())
                    .currency("ARS")
                    .priority(suggestion.priority() == null
                            ? MonthlyPlanItem.Priority.IMPORTANT
                            : suggestion.priority())
                    .status(MonthlyPlanItem.Status.ESTIMATED)
                    .source(suggestion.source() == null
                            ? MonthlyPlanItem.Source.SYSTEM
                            : suggestion.source())
                    .build();

            try {
                validateSuggestedPlanItem(profileId, item);
            } catch (RuntimeException ex) {
                errors.add("Planificación '" + suggestion.title() + "': " + ex.getMessage());
                continue;
            }

            boolean duplicate = isDuplicatePlanItem(
                    profileId,
                    item.getPeriodYear(),
                    item.getPeriodMonth(),
                    item.getTitle(),
                    item.getAmount(),
                    item.getMinAmount(),
                    item.getMaxAmount(),
                    item.getCategoryId(),
                    item.getSource()
            );

            if (duplicate && Boolean.TRUE.equals(request.skipDuplicates())) {
                skippedDuplicates++;
                warnings.add("Planificación duplicada omitida: " + item.getTitle() + ".");
                continue;
            }

            item.setAmount(nonNegativeOrNull(item.getAmount()));
            item.setMinAmount(nonNegativeOrNull(item.getMinAmount()));
            item.setMaxAmount(nonNegativeOrNull(item.getMaxAmount()));

            monthlyPlanItemRepository.save(item);
            createdMonthlyPlanItems++;
        }

        return new BudgetPlanningSuggestionCommitResponse(
                createdBudgetItems,
                updatedBudgetItems,
                createdMonthlyPlanItems,
                skippedDuplicates,
                warnings,
                errors
        );
    }

    private List<BudgetSuggestion> buildBudgetSuggestions(
            List<MoneyTransaction> transactions,
            Map<UUID, Category> categoriesById,
            Set<UUID> selectedIds,
            BudgetPlanningSuggestionPreviewRequest request,
            SuggestionWindow window
    ) {
        BigDecimal roundingMultiple = roundingMultiple(request.roundingMultiple());
        YearMonth targetMonth = YearMonth.of(request.year(), request.month());

        Map<UUID, List<MoneyTransaction>> byCategory = transactions.stream()
                .filter(tx -> includeByOrigin(tx, request))
                .filter(tx -> isBudgetCandidate(tx, categoriesById.get(tx.getCategoryId()), selectedIds, request))
                .collect(Collectors.groupingBy(
                        MoneyTransaction::getCategoryId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<BudgetSuggestion> suggestions = new ArrayList<>();

        for (var entry : byCategory.entrySet()) {
            Category category = categoriesById.get(entry.getKey());
            List<MoneyTransaction> categoryTransactions = entry.getValue();
            Map<YearMonth, BigDecimal> sumsByMonth = sumsByMonth(categoryTransactions);
            OutlierResult detectedOutlier = detectOutlier(categoryTransactions, sumsByMonth, targetMonth);
            boolean outlierAffectsSuggestedAmount = detectedOutlier.outlierDetected()
                    && (request.mode() != SuggestionMode.CURRENT_MONTH_ONLY
                    || targetMonth.equals(detectedOutlier.outlierMonth()));
            OutlierResult outlier = new OutlierResult(
                    detectedOutlier.outlierDetected(),
                    outlierAffectsSuggestedAmount,
                    detectedOutlier.outlierMonth()
            );
            YearMonth excludedOutlierMonth = outlier.outlierAffectsSuggestedAmount()
                    ? outlier.outlierMonth()
                    : null;

            BigDecimal realAmount = sumsByMonth.getOrDefault(targetMonth, BigDecimal.ZERO);
            BigDecimal basis = calculateBudgetBasis(sumsByMonth, request.mode(), window, targetMonth, excludedOutlierMonth);
            BigDecimal suggestedAmount = roundMoney(basis, roundingMultiple);

            int transactionCount = categoryTransactions.size();
            int monthsWithData = (int) sumsByMonth.values().stream()
                    .filter(amount -> amount.signum() > 0)
                    .count();

            SuggestionConfidence confidence = budgetConfidence(
                    request.mode(),
                    monthsWithData,
                    transactionCount,
                    outlier.outlierDetected()
            );

            suggestions.add(new BudgetSuggestion(
                    category.getId(),
                    category.getName(),
                    category.getType(),
                    realAmount.setScale(2, RoundingMode.HALF_UP),
                    suggestedAmount,
                    transactionCount,
                    confidence,
                    budgetReason(request.mode(), monthsWithData, outlier),
                    outlier.outlierDetected(),
                    outlier.outlierAffectsSuggestedAmount(),
                    !outlier.outlierAffectsSuggestedAmount() && confidence != SuggestionConfidence.NONE,
                    categoryTransactions.stream().map(MoneyTransaction::getId).filter(Objects::nonNull).toList()
            ));
        }

        suggestions.sort(Comparator.comparing(BudgetSuggestion::categoryName));
        return suggestions;
    }

    private List<MonthlyPlanSuggestion> buildMonthlyPlanSuggestions(
            UUID profileId,
            List<MoneyTransaction> transactions,
            Map<UUID, Category> categoriesById,
            Map<UUID, Account> accountsById,
            Set<UUID> selectedIds,
            BudgetPlanningSuggestionPreviewRequest request,
            SuggestionWindow window,
            List<String> warnings
    ) {
        YearMonth requestedMonth = YearMonth.of(request.year(), request.month());
        YearMonth targetMonth = Boolean.TRUE.equals(request.nextMonth())
                ? requestedMonth.plusMonths(1)
                : requestedMonth;
        BigDecimal roundingMultiple = roundingMultiple(request.roundingMultiple());

        List<MonthlyPlanItem> existingItems = monthlyPlanItemRepository
                .findByProfileIdAndPeriodYearAndPeriodMonth(
                        profileId,
                        targetMonth.getYear(),
                        targetMonth.getMonthValue()
                );

        List<MoneyTransaction> targetConfirmedTransactions = transactionRepository
                .findByProfileIdAndBudgetDateBetween(
                        profileId,
                        targetMonth.atDay(1),
                        targetMonth.atEndOfMonth()
                )
                .stream()
                .filter(tx -> tx.getStatus() == MoneyTransaction.Status.CONFIRMED)
                .toList();

        Map<PlanGroupKey, List<MoneyTransaction>> groups = new LinkedHashMap<>();

        for (MoneyTransaction tx : transactions) {
            Category category = categoriesById.get(tx.getCategoryId());

            if (!includeByOrigin(tx, request)
                    || !isPlanSourceCandidate(tx, category, selectedIds, request)) {
                continue;
            }

            MonthlyPlanItem.Type type = inferPlanType(tx, category);

            if (type == null || type == MonthlyPlanItem.Type.TRANSFER || type == MonthlyPlanItem.Type.TODO) {
                continue;
            }

            String title = buildPlanTitle(tx, category, type);
            PlanGroupKey key = new PlanGroupKey(type, tx.getCategoryId(), tx.getAccountId(), normalize(title));
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(tx);
        }

        List<MonthlyPlanSuggestion> suggestions = new ArrayList<>();

        for (var entry : groups.entrySet()) {
            List<MoneyTransaction> group = entry.getValue();
            Category category = categoriesById.get(entry.getKey().categoryId());

            if (!isCommitmentGroup(group, category)) {
                continue;
            }

            BigDecimal basis = calculateBudgetBasis(
                    sumsByMonth(group),
                    request.mode(),
                    window,
                    requestedMonth,
                    null
            );
            BigDecimal amount = roundMoney(basis, roundingMultiple);

            if (amount.signum() <= 0) {
                continue;
            }

            String title = buildPlanTitle(group.get(0), category, entry.getKey().type());

            if (hasConfirmedRealDuplicate(targetConfirmedTransactions, title, amount, entry.getKey().categoryId())) {
                warnings.add("No se sugirió planificación para '" + title + "' porque ya existe un movimiento real confirmado en el período destino.");
                continue;
            }

            MonthlyPlanItem.Source source = group.stream().anyMatch(this::isImported)
                    ? MonthlyPlanItem.Source.IMPORT
                    : MonthlyPlanItem.Source.SYSTEM;
            boolean duplicate = existingItems.stream().anyMatch(existing ->
                    matchesPlanDuplicate(
                            existing,
                            title,
                            amount,
                            null,
                            null,
                            entry.getKey().categoryId(),
                            source
                    )
            );
            int monthsWithData = (int) sumsByMonth(group).values().stream()
                    .filter(value -> value.signum() > 0)
                    .count();
            SuggestionConfidence confidence = planConfidence(group, category, monthsWithData, duplicate);
            Account account = accountsById.get(entry.getKey().accountId());
            LocalDate expectedDate = inferExpectedDate(group, targetMonth);

            suggestions.add(new MonthlyPlanSuggestion(
                    title,
                    buildPlanDescription(group, request.mode()),
                    expectedDate,
                    targetMonth.getYear(),
                    targetMonth.getMonthValue(),
                    amount,
                    null,
                    null,
                    entry.getKey().categoryId(),
                    category == null ? null : category.getName(),
                    entry.getKey().accountId(),
                    account == null ? null : account.getName(),
                    entry.getKey().type(),
                    inferPriority(entry.getKey().type(), category, title),
                    source,
                    confidence,
                    planReason(entry.getKey().type(), monthsWithData, duplicate),
                    duplicate,
                    !duplicate && confidence != SuggestionConfidence.NONE,
                    group.stream().map(MoneyTransaction::getId).filter(Objects::nonNull).toList()
            ));
        }

        suggestions.sort(Comparator
                .comparing(MonthlyPlanSuggestion::periodYear)
                .thenComparing(MonthlyPlanSuggestion::periodMonth)
                .thenComparing(suggestion -> suggestion.expectedDate() == null ? LocalDate.MAX : suggestion.expectedDate())
                .thenComparing(MonthlyPlanSuggestion::title));

        return suggestions;
    }

    private BudgetPlanningSuggestionTotals buildTotals(
            List<BudgetSuggestion> budgetSuggestions,
            List<MonthlyPlanSuggestion> monthlyPlanSuggestions
    ) {
        BigDecimal totalBudgetReal = budgetSuggestions.stream()
                .map(BudgetSuggestion::realAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBudgetSuggested = budgetSuggestions.stream()
                .map(BudgetSuggestion::suggestedBudgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPlan = monthlyPlanSuggestions.stream()
                .map(MonthlyPlanSuggestion::amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int outliers = (int) budgetSuggestions.stream()
                .filter(suggestion -> Boolean.TRUE.equals(suggestion.outlierDetected()))
                .count();

        return new BudgetPlanningSuggestionTotals(
                totalBudgetReal,
                totalBudgetSuggested,
                budgetSuggestions.size(),
                totalPlan,
                monthlyPlanSuggestions.size(),
                outliers
        );
    }

    private List<MoneyTransaction> loadTransactions(
            UUID profileId,
            BudgetPlanningSuggestionPreviewRequest request,
            SuggestionWindow window
    ) {
        if (request.selectedTransactionIds() != null && !request.selectedTransactionIds().isEmpty()) {
            return transactionRepository.findByProfileIdAndIdIn(profileId, request.selectedTransactionIds());
        }

        return transactionRepository.findByProfileIdAndBudgetDateBetween(
                profileId,
                window.start().atDay(1),
                window.end().atEndOfMonth()
        );
    }

    private Map<UUID, Category> loadCategories(List<MoneyTransaction> transactions) {
        List<UUID> ids = transactions.stream()
                .map(MoneyTransaction::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        return categoryRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    private Map<UUID, Account> loadAccounts(List<MoneyTransaction> transactions) {
        List<UUID> ids = transactions.stream()
                .map(MoneyTransaction::getAccountId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        return accountRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
    }

    private boolean isBudgetCandidate(
            MoneyTransaction tx,
            Category category,
            Set<UUID> selectedIds,
            BudgetPlanningSuggestionPreviewRequest request
    ) {
        if (!isBaseConfirmedCandidate(tx, category, selectedIds, request)) {
            return false;
        }

        if (!isBudgetableCategory(category)) {
            return false;
        }

        if (tx.getMovementType() == MoneyTransaction.MovementType.EXPENSE) {
            return category.getType() == Category.Type.FIXED_EXPENSE
                    || category.getType() == Category.Type.VARIABLE_EXPENSE
                    || category.getType() == Category.Type.DEBT;
        }

        if (tx.getMovementType() == MoneyTransaction.MovementType.SAVING) {
            return category.getType() == Category.Type.SAVING
                    || category.getType() == Category.Type.INVESTMENT;
        }

        return tx.getMovementType() == MoneyTransaction.MovementType.ADJUSTMENT
                && category.getType() == Category.Type.DEBT;
    }

    private boolean isPlanSourceCandidate(
            MoneyTransaction tx,
            Category category,
            Set<UUID> selectedIds,
            BudgetPlanningSuggestionPreviewRequest request
    ) {
        if (!isBaseConfirmedCandidate(tx, category, selectedIds, request)) {
            return false;
        }

        if (!isPlanCategoryAllowed(category)) {
            return false;
        }

        return tx.getMovementType() == MoneyTransaction.MovementType.INCOME
                || tx.getMovementType() == MoneyTransaction.MovementType.EXPENSE
                || tx.getMovementType() == MoneyTransaction.MovementType.SAVING
                || tx.getMovementType() == MoneyTransaction.MovementType.ADJUSTMENT;
    }

    private boolean isBaseConfirmedCandidate(
            MoneyTransaction tx,
            Category category,
            Set<UUID> selectedIds,
            BudgetPlanningSuggestionPreviewRequest request
    ) {
        if (tx.getStatus() != MoneyTransaction.Status.CONFIRMED) {
            return false;
        }

        if (tx.getCategoryId() == null || category == null) {
            return false;
        }

        if (tx.getMovementType() == MoneyTransaction.MovementType.TRANSFER) {
            return false;
        }

        if (tx.getPaymentChannel() == MoneyTransaction.PaymentChannel.INTERNAL_TRANSFER
                || tx.getInternalTransferGroupId() != null) {
            return false;
        }

        MoneyTransaction.ClassificationStatus classificationStatus = tx.getClassificationStatus();

        if (classificationStatus == MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY
                || classificationStatus == MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE
                || classificationStatus == MoneyTransaction.ClassificationStatus.TECHNICAL) {
            return false;
        }

        boolean explicitlySelected = tx.getId() != null && selectedIds.contains(tx.getId());

        if (classificationStatus == MoneyTransaction.ClassificationStatus.REVIEW
                && !Boolean.TRUE.equals(request.includeReview())
                && !explicitlySelected) {
            return false;
        }

        String reason = normalizeCode(tx.getClassificationReason());

        if (EXCLUDED_CLASSIFICATION_REASONS.contains(reason)) {
            return false;
        }

        if ("CJPRESTAMOS".equals(normalizeCode(tx.getSource()))
                && (CJ_EXCLUDED_REASONS.contains(reason)
                || containsNormalized(tx.getSourceOperationId(), "DISBURSEMENT")
                || containsNormalized(tx.getSourceOperationId(), "RECOVERABLE_OUTFLOW"))) {
            return false;
        }

        return true;
    }

    private boolean includeByOrigin(MoneyTransaction tx, BudgetPlanningSuggestionPreviewRequest request) {
        boolean imported = isImported(tx);

        if (Boolean.TRUE.equals(request.includeImportedOnly())) {
            return imported;
        }

        if (Boolean.FALSE.equals(request.includeManual()) && !imported) {
            return false;
        }

        return true;
    }

    private boolean isImported(MoneyTransaction tx) {
        return tx.getOrigin() == MoneyTransaction.Origin.IMPORT
                || tx.getImportBatchId() != null
                || (tx.getSource() != null && !tx.getSource().isBlank());
    }

    private boolean isBudgetableCategory(Category category) {
        return category != null
                && Boolean.TRUE.equals(category.getActive())
                && Boolean.TRUE.equals(category.getBudgetable())
                && !Boolean.TRUE.equals(category.getTechnical())
                && (category.getType() == Category.Type.FIXED_EXPENSE
                || category.getType() == Category.Type.VARIABLE_EXPENSE
                || category.getType() == Category.Type.SAVING
                || category.getType() == Category.Type.DEBT
                || category.getType() == Category.Type.INVESTMENT);
    }

    private boolean isPlanCategoryAllowed(Category category) {
        return category != null
                && Boolean.TRUE.equals(category.getActive())
                && !Boolean.TRUE.equals(category.getTechnical());
    }

    private Map<YearMonth, BigDecimal> sumsByMonth(List<MoneyTransaction> transactions) {
        Map<YearMonth, BigDecimal> sums = new HashMap<>();

        for (MoneyTransaction tx : transactions) {
            if (tx.getBudgetDate() == null || tx.getAmount() == null) {
                continue;
            }

            YearMonth month = YearMonth.from(tx.getBudgetDate());
            sums.merge(month, tx.getAmount().abs(), BigDecimal::add);
        }

        return sums;
    }

    private BigDecimal calculateBudgetBasis(
            Map<YearMonth, BigDecimal> sumsByMonth,
            SuggestionMode mode,
            SuggestionWindow window,
            YearMonth targetMonth,
            YearMonth excludedOutlierMonth
    ) {
        if (mode == SuggestionMode.CURRENT_MONTH_ONLY) {
            BigDecimal current = excludedOutlierMonth != null && excludedOutlierMonth.equals(targetMonth)
                    ? BigDecimal.ZERO
                    : sumsByMonth.getOrDefault(targetMonth, BigDecimal.ZERO);

            if (current.signum() > 0 || excludedOutlierMonth == null) {
                return current;
            }

            return averageAvailableMonths(sumsByMonth, window, excludedOutlierMonth);
        }

        return averageAvailableMonths(sumsByMonth, window, excludedOutlierMonth);
    }

    private BigDecimal averageAvailableMonths(
            Map<YearMonth, BigDecimal> sumsByMonth,
            SuggestionWindow window,
            YearMonth excludedOutlierMonth
    ) {
        BigDecimal total = BigDecimal.ZERO;
        int months = 0;

        for (YearMonth cursor = window.start(); !cursor.isAfter(window.end()); cursor = cursor.plusMonths(1)) {
            if (cursor.equals(excludedOutlierMonth)) {
                continue;
            }

            BigDecimal amount = sumsByMonth.getOrDefault(cursor, BigDecimal.ZERO);

            if (amount.signum() > 0) {
                total = total.add(amount);
                months++;
            }
        }

        if (months == 0) {
            return BigDecimal.ZERO;
        }

        return total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }

    private OutlierResult detectOutlier(
            List<MoneyTransaction> transactions,
            Map<YearMonth, BigDecimal> sumsByMonth,
            YearMonth targetMonth
    ) {
        Map<YearMonth, Long> countByMonth = transactions.stream()
                .filter(tx -> tx.getBudgetDate() != null)
                .collect(Collectors.groupingBy(tx -> YearMonth.from(tx.getBudgetDate()), Collectors.counting()));

        YearMonth outlierMonth = null;
        BigDecimal strongestOutlier = BigDecimal.ZERO;

        for (var entry : sumsByMonth.entrySet()) {
            BigDecimal amount = entry.getValue();
            List<BigDecimal> otherMonths = sumsByMonth.entrySet().stream()
                    .filter(other -> !other.getKey().equals(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .filter(value -> value.signum() > 0)
                    .sorted()
                    .toList();

            if (otherMonths.size() < 2 || countByMonth.getOrDefault(entry.getKey(), 0L) != 1L) {
                continue;
            }

            BigDecimal median = median(otherMonths);

            if (median.signum() > 0
                    && amount.compareTo(median.multiply(OUTLIER_MULTIPLIER)) >= 0
                    && amount.compareTo(strongestOutlier) > 0) {
                outlierMonth = entry.getKey();
                strongestOutlier = amount;
            }
        }

        boolean affectsSuggestedAmount = outlierMonth != null;

        return new OutlierResult(
                outlierMonth != null,
                affectsSuggestedAmount,
                outlierMonth
        );
    }

    private SuggestionConfidence budgetConfidence(
            SuggestionMode mode,
            int monthsWithData,
            int transactionCount,
            boolean outlier
    ) {
        if (outlier) {
            return SuggestionConfidence.LOW;
        }

        if (mode != SuggestionMode.CURRENT_MONTH_ONLY && monthsWithData >= 3) {
            return SuggestionConfidence.HIGH;
        }

        if (transactionCount >= 2 || monthsWithData >= 2) {
            return SuggestionConfidence.MEDIUM;
        }

        return SuggestionConfidence.LOW;
    }

    private String budgetReason(SuggestionMode mode, int monthsWithData, OutlierResult outlier) {
        String base = switch (mode) {
            case CURRENT_MONTH_ONLY -> "Suma del mes seleccionado.";
            case LAST_3_MONTHS_AVERAGE -> "Promedio de los últimos 3 meses disponibles.";
            case LAST_6_MONTHS_AVERAGE -> "Promedio de los últimos 6 meses disponibles.";
        };

        if (outlier.outlierAffectsSuggestedAmount()) {
            return base + " Se detectó un gasto atípico de una sola vez y se excluyó del importe sugerido.";
        }

        if (outlier.outlierDetected()) {
            return base + " Se detectó un gasto atípico que no modifica el importe sugerido.";
        }

        return base + " Meses con movimientos: " + monthsWithData + ".";
    }

    private MonthlyPlanItem.Type inferPlanType(MoneyTransaction tx, Category category) {
        if (tx.getMovementType() == MoneyTransaction.MovementType.INCOME
                || category.getType() == Category.Type.INCOME) {
            return MonthlyPlanItem.Type.INCOME;
        }

        if (category.getType() == Category.Type.DEBT
                || tx.getPaymentChannel() == MoneyTransaction.PaymentChannel.CREDIT_CARD
                || tx.getPaymentChannel() == MoneyTransaction.PaymentChannel.MERCADO_CREDITO
                || containsCommitmentKeyword(tx, "credito")
                || containsCommitmentKeyword(tx, "prestamo")
                || containsCommitmentKeyword(tx, "tarjeta")) {
            return MonthlyPlanItem.Type.DEBT;
        }

        if (tx.getMovementType() == MoneyTransaction.MovementType.SAVING
                || category.getType() == Category.Type.SAVING
                || category.getType() == Category.Type.INVESTMENT) {
            return MonthlyPlanItem.Type.SAVING;
        }

        if (tx.getMovementType() == MoneyTransaction.MovementType.EXPENSE) {
            return MonthlyPlanItem.Type.EXPENSE;
        }

        return null;
    }

    private boolean isCommitmentGroup(List<MoneyTransaction> group, Category category) {
        Set<YearMonth> months = group.stream()
                .map(MoneyTransaction::getBudgetDate)
                .filter(Objects::nonNull)
                .map(YearMonth::from)
                .collect(Collectors.toSet());

        boolean recurrent = months.size() >= 2;
        boolean commitmentByKind = category != null
                && (category.getType() == Category.Type.FIXED_EXPENSE
                || category.getType() == Category.Type.DEBT
                || category.getType() == Category.Type.SAVING
                || category.getType() == Category.Type.INVESTMENT
                || category.getType() == Category.Type.INCOME);
        boolean keyword = group.stream().anyMatch(this::hasAnyCommitmentKeyword);

        if (category != null && category.getType() == Category.Type.VARIABLE_EXPENSE) {
            return recurrent || keyword;
        }

        return recurrent || commitmentByKind || keyword;
    }

    private String buildPlanTitle(
            MoneyTransaction tx,
            Category category,
            MonthlyPlanItem.Type type
    ) {
        String description = tx.getDescription() == null ? "" : tx.getDescription().trim();

        if ("BANCO_PROVINCIA".equals(normalizeCode(tx.getSource()))
                && (type == MonthlyPlanItem.Type.DEBT || containsNormalized(description, "PRESTAMO"))) {
            return "Banco Provincia préstamo";
        }

        if (description.isBlank()) {
            return category == null ? "Compromiso mensual" : category.getName();
        }

        String clean = description
                .replaceAll("\\|.*$", "")
                .replaceAll("\\s+", " ")
                .trim();

        if (clean.length() > 80) {
            clean = clean.substring(0, 80).trim();
        }

        return clean.isBlank() ? category.getName() : clean;
    }

    private String buildPlanDescription(List<MoneyTransaction> group, SuggestionMode mode) {
        String modeLabel = switch (mode) {
            case CURRENT_MONTH_ONLY -> "mes seleccionado";
            case LAST_3_MONTHS_AVERAGE -> "últimos 3 meses";
            case LAST_6_MONTHS_AVERAGE -> "últimos 6 meses";
        };

        return "Sugerido desde movimientos confirmados del " + modeLabel
                + ". Operaciones tomadas: " + group.size() + ".";
    }

    private LocalDate inferExpectedDate(List<MoneyTransaction> group, YearMonth targetMonth) {
        Optional<Integer> day = group.stream()
                .map(MoneyTransaction::getRealDate)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(LocalDate::getDayOfMonth);

        int safeDay = Math.min(day.orElse(1), targetMonth.lengthOfMonth());
        return targetMonth.atDay(safeDay);
    }

    private MonthlyPlanItem.Priority inferPriority(
            MonthlyPlanItem.Type type,
            Category category,
            String title
    ) {
        if (type == MonthlyPlanItem.Type.DEBT
                || containsNormalized(title, "ALQUILER")
                || containsNormalized(title, "EXPENSAS")) {
            return MonthlyPlanItem.Priority.ESSENTIAL;
        }

        if (category != null && category.getType() == Category.Type.FIXED_EXPENSE) {
            return MonthlyPlanItem.Priority.ESSENTIAL;
        }

        return MonthlyPlanItem.Priority.IMPORTANT;
    }

    private SuggestionConfidence planConfidence(
            List<MoneyTransaction> group,
            Category category,
            int monthsWithData,
            boolean duplicate
    ) {
        if (duplicate) {
            return SuggestionConfidence.LOW;
        }

        if (monthsWithData >= 3) {
            return SuggestionConfidence.HIGH;
        }

        if (monthsWithData >= 2
                || group.stream().anyMatch(this::hasAnyCommitmentKeyword)
                || (category != null && category.getType() == Category.Type.DEBT)) {
            return SuggestionConfidence.MEDIUM;
        }

        return SuggestionConfidence.LOW;
    }

    private String planReason(MonthlyPlanItem.Type type, int monthsWithData, boolean duplicate) {
        String typeReason = switch (type) {
            case INCOME -> "Ingreso recurrente detectado.";
            case DEBT -> "Compromiso de deuda/tarjeta detectado.";
            case SAVING -> "Ahorro o inversión programable detectado.";
            case RECOVERY -> "Recupero esperado detectado.";
            default -> "Compromiso recurrente detectado.";
        };

        if (duplicate) {
            return typeReason + " Ya hay un ítem similar en el período destino.";
        }

        return typeReason + " Meses con movimientos: " + monthsWithData + ".";
    }

    private boolean hasConfirmedRealDuplicate(
            List<MoneyTransaction> targetTransactions,
            String title,
            BigDecimal amount,
            UUID categoryId
    ) {
        return targetTransactions.stream().anyMatch(tx ->
                Objects.equals(tx.getCategoryId(), categoryId)
                        && similarAmount(amount, tx.getAmount())
                        && (sharedTokenCount(normalize(title), normalize(tx.getDescription())) > 0
                        || normalize(title).equals(normalize(tx.getDescription())))
        );
    }

    private boolean isDuplicatePlanItem(
            UUID profileId,
            int year,
            int month,
            String title,
            BigDecimal amount,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            UUID categoryId,
            MonthlyPlanItem.Source source
    ) {
        return monthlyPlanItemRepository.findByProfileIdAndPeriodYearAndPeriodMonth(profileId, year, month)
                .stream()
                .anyMatch(existing -> matchesPlanDuplicate(
                        existing,
                        title,
                        amount,
                        minAmount,
                        maxAmount,
                        categoryId,
                        source
                ));
    }

    private boolean matchesPlanDuplicate(
            MonthlyPlanItem existing,
            String title,
            BigDecimal amount,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            UUID categoryId,
            MonthlyPlanItem.Source source
    ) {
        if (existing.getStatus() == MonthlyPlanItem.Status.CANCELLED) {
            return false;
        }

        BigDecimal candidateAmount = firstNonNull(amount, minAmount, maxAmount, BigDecimal.ZERO);
        BigDecimal existingAmount = firstNonNull(
                existing.getAmount(),
                existing.getMinAmount(),
                existing.getMaxAmount(),
                BigDecimal.ZERO
        );

        boolean strict = normalize(existing.getTitle()).equals(normalize(title))
                && Objects.equals(existing.getCategoryId(), categoryId)
                && existing.getSource() == source
                && similarAmount(candidateAmount, existingAmount);

        boolean similar = Objects.equals(existing.getCategoryId(), categoryId)
                && similarAmount(candidateAmount, existingAmount)
                && sharedTokenCount(normalize(existing.getTitle()), normalize(title)) >= 1;

        return strict || similar;
    }

    private ValidatedBudgetSuggestion validateBudgetSuggestion(
            UUID profileId,
            ApplyBudgetSuggestion suggestion
    ) {
        if (suggestion.categoryId() == null) {
            throw new BadRequestException("Categoría requerida");
        }

        if (suggestion.suggestedBudgetAmount() == null) {
            throw new BadRequestException("Monto requerido para " + suggestion.categoryId());
        }

        if (suggestion.suggestedBudgetAmount().signum() < 0) {
            throw new BadRequestException("Monto negativo para " + suggestion.categoryId());
        }

        Category category = findCategoryForProfile(suggestion.categoryId(), profileId)
                .orElseThrow(() -> new BadRequestException("Categoría no válida para presupuesto: " + suggestion.categoryId()));

        if (!isBudgetableCategory(category)) {
            throw new BadRequestException("Categoría no válida para presupuesto: " + category.getName());
        }

        return new ValidatedBudgetSuggestion(suggestion, category);
    }

    private void validateSuggestedPlanItem(UUID profileId, MonthlyPlanItem item) {
        monthlyPlanItemValidator.validate(item, SUGGESTION_ALLOWED_PLAN_TYPES, true);
        monthlyPlanItemValidator.validateReferences(
                profileId,
                item.getAccountId(),
                item.getCategoryId(),
                true,
                true
        );
    }

    private Optional<Category> findCategoryForProfile(UUID categoryId, UUID profileId) {
        return categoryRepository.findById(categoryId)
                .filter(category -> category.getProfileId() == null
                        || Objects.equals(category.getProfileId(), profileId));
    }

    private BudgetMonth ensureBudgetMonth(UUID profileId, Integer year, Integer month) {
        BudgetYear budgetYear = budgetYearRepository.findByProfileIdAndYear(profileId, year)
                .orElseGet(() -> budgetYearRepository.save(
                        BudgetYear.builder()
                                .profileId(profileId)
                                .year(year)
                                .build()
                ));

        return budgetMonthRepository.findByBudgetYearIdAndMonth(budgetYear.getId(), month)
                .orElseGet(() -> budgetMonthRepository.save(
                        BudgetMonth.builder()
                                .budgetYearId(budgetYear.getId())
                                .month(month)
                                .build()
                ));
    }

    private void ensureProfile(UUID profileId, UUID userId) {
        profileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ForbiddenException("Profile does not belong to user"));
    }

    private void validatePeriod(Integer year, Integer month) {
        if (year == null || year < 2000 || year > 2100 || month == null || month < 1 || month > 12) {
            throw new BadRequestException("Período inválido");
        }
    }

    private SuggestionWindow windowFor(Integer year, Integer month, SuggestionMode mode) {
        YearMonth end = YearMonth.of(year, month);
        int months = switch (mode) {
            case CURRENT_MONTH_ONLY -> 1;
            case LAST_3_MONTHS_AVERAGE -> 3;
            case LAST_6_MONTHS_AVERAGE -> 6;
        };

        return new SuggestionWindow(end.minusMonths(months - 1L), end, months);
    }

    private BigDecimal roundingMultiple(BigDecimal requested) {
        if (requested == null || requested.signum() <= 0) {
            return DEFAULT_ROUNDING_MULTIPLE;
        }

        return requested;
    }

    private BigDecimal roundMoney(BigDecimal amount, BigDecimal multiple) {
        if (amount == null || amount.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return amount
                .divide(multiple, 0, RoundingMode.HALF_UP)
                .multiply(multiple)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal median(List<BigDecimal> sortedValues) {
        if (sortedValues.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int middle = sortedValues.size() / 2;

        if (sortedValues.size() % 2 == 1) {
            return sortedValues.get(middle);
        }

        return sortedValues.get(middle - 1)
                .add(sortedValues.get(middle))
                .divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
    }

    private boolean similarAmount(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }

        BigDecimal normalizedLeft = left.abs();
        BigDecimal normalizedRight = right.abs();

        if (normalizedLeft.compareTo(normalizedRight) == 0) {
            return true;
        }

        BigDecimal base = normalizedRight.max(BigDecimal.ONE);
        BigDecimal tolerance = base.multiply(AMOUNT_SIMILARITY_PERCENT).max(MIN_SIMILARITY_TOLERANCE);

        return normalizedLeft.subtract(normalizedRight).abs().compareTo(tolerance) <= 0;
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value == null || value.signum() < 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegativeOrNull(BigDecimal value) {
        if (value == null) {
            return null;
        }

        return nonNegative(value);
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private boolean hasAnyCommitmentKeyword(MoneyTransaction tx) {
        String normalized = normalize((tx.getDescription() == null ? "" : tx.getDescription())
                + " "
                + (tx.getCounterparty() == null ? "" : tx.getCounterparty()));

        return COMMITMENT_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    private boolean containsCommitmentKeyword(MoneyTransaction tx, String keyword) {
        return normalize((tx.getDescription() == null ? "" : tx.getDescription())
                + " "
                + (tx.getCounterparty() == null ? "" : tx.getCounterparty()))
                .contains(normalize(keyword));
    }

    private boolean containsNormalized(String value, String token) {
        if (value == null || token == null) {
            return false;
        }

        return normalize(value).contains(normalize(token));
    }

    private String normalizeCode(String value) {
        return value == null
                ? ""
                : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String clean = Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        return NON_ALNUM.matcher(clean)
                .replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int sharedTokenCount(String left, String right) {
        Set<String> leftTokens = tokens(left);
        Set<String> rightTokens = tokens(right);
        leftTokens.retainAll(rightTokens);
        return leftTokens.size();
    }

    private Set<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return new HashSet<>();
        }

        return java.util.Arrays.stream(value.split(" "))
                .filter(token -> token.length() > 2)
                .filter(token -> !token.chars().allMatch(Character::isDigit))
                .filter(token -> !GENERIC_DUPLICATE_TOKENS.contains(token))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Set<UUID> selectedIds(List<UUID> ids) {
        return ids == null ? Set.of() : new HashSet<>(ids);
    }

    private record SuggestionWindow(YearMonth start, YearMonth end, int months) {
    }

    private record OutlierResult(
            boolean outlierDetected,
            boolean outlierAffectsSuggestedAmount,
            YearMonth outlierMonth
    ) {
    }

    private record ValidatedBudgetSuggestion(
            ApplyBudgetSuggestion suggestion,
            Category category
    ) {
    }

    private record PlanGroupKey(
            MonthlyPlanItem.Type type,
            UUID categoryId,
            UUID accountId,
            String normalizedTitle
    ) {
    }
}
