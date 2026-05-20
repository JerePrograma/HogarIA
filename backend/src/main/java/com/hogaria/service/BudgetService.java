package com.hogaria.service;

import com.hogaria.dto.BudgetDtos.BudgetCategoryItemResponse;
import com.hogaria.dto.BudgetDtos.BudgetCategoryItemUpsertRequest;
import com.hogaria.dto.BudgetDtos.BudgetComparisonItemResponse;
import com.hogaria.dto.BudgetDtos.BudgetComparisonResponse;
import com.hogaria.dto.BudgetDtos.BudgetMonthCreateRequest;
import com.hogaria.dto.BudgetDtos.BudgetMonthResponse;
import com.hogaria.dto.BudgetDtos.BudgetMonthUpdateRequest;
import com.hogaria.dto.BudgetDtos.BudgetStatus;
import com.hogaria.dto.BudgetDtos.BudgetYearCreateRequest;
import com.hogaria.dto.BudgetDtos.BudgetYearResponse;
import com.hogaria.dto.BudgetDtos.BudgetYearUpdateRequest;
import com.hogaria.entity.BudgetCategoryItem;
import com.hogaria.entity.BudgetMonth;
import com.hogaria.entity.BudgetYear;
import com.hogaria.entity.Category;
import com.hogaria.entity.MoneyTransaction;
import com.hogaria.exception.ConflictException;
import com.hogaria.exception.ForbiddenException;
import com.hogaria.exception.NotFoundException;
import com.hogaria.repository.BudgetCategoryItemRepository;
import com.hogaria.repository.BudgetMonthRepository;
import com.hogaria.repository.BudgetYearRepository;
import com.hogaria.repository.CategoryRepository;
import com.hogaria.repository.FinancialProfileRepository;
import com.hogaria.repository.MoneyTransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal WARNING_PERCENTAGE = new BigDecimal("80");

    private final FinancialProfileRepository financialProfileRepository;
    private final BudgetYearRepository budgetYearRepository;
    private final BudgetMonthRepository budgetMonthRepository;
    private final BudgetCategoryItemRepository budgetCategoryItemRepository;
    private final CategoryRepository categoryRepository;
    private final MoneyTransactionRepository transactionRepository;

    public BudgetService(
            FinancialProfileRepository financialProfileRepository,
            BudgetYearRepository budgetYearRepository,
            BudgetMonthRepository budgetMonthRepository,
            BudgetCategoryItemRepository budgetCategoryItemRepository,
            CategoryRepository categoryRepository,
            MoneyTransactionRepository transactionRepository
    ) {
        this.financialProfileRepository = financialProfileRepository;
        this.budgetYearRepository = budgetYearRepository;
        this.budgetMonthRepository = budgetMonthRepository;
        this.budgetCategoryItemRepository = budgetCategoryItemRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public BudgetYearResponse createBudgetYear(
            UUID userId,
            UUID profileId,
            BudgetYearCreateRequest request
    ) {
        assertProfileOwner(profileId, userId);

        if (budgetYearRepository.existsByProfileIdAndYear(profileId, request.year())) {
            throw new ConflictException("Budget year already exists");
        }

        BudgetYear budgetYear = BudgetYear.builder()
                .profileId(profileId)
                .year(request.year())
                .targetIncome(request.targetIncome())
                .targetSaving(request.targetSaving())
                .notes(request.notes())
                .build();

        return toBudgetYearResponse(budgetYearRepository.save(budgetYear));
    }

    @Transactional(readOnly = true)
    public List<BudgetYearResponse> listBudgetYears(UUID userId, UUID profileId) {
        assertProfileOwner(profileId, userId);

        return budgetYearRepository.findByProfileId(profileId)
                .stream()
                .sorted(Comparator.comparing(BudgetYear::getYear).reversed())
                .map(this::toBudgetYearResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetYearResponse getBudgetYear(UUID userId, UUID profileId, Integer year) {
        assertProfileOwner(profileId, userId);

        BudgetYear budgetYear = findBudgetYear(profileId, year);

        return toBudgetYearResponse(budgetYear);
    }

    @Transactional
    public BudgetYearResponse updateBudgetYear(
            UUID userId,
            UUID profileId,
            Integer year,
            BudgetYearUpdateRequest request
    ) {
        assertProfileOwner(profileId, userId);

        BudgetYear budgetYear = findBudgetYear(profileId, year);

        budgetYear.setTargetIncome(request.targetIncome());
        budgetYear.setTargetSaving(request.targetSaving());
        budgetYear.setNotes(request.notes());

        return toBudgetYearResponse(budgetYearRepository.save(budgetYear));
    }

    /**
     * Semántica elegida:
     * - Si el año no existe, lo crea.
     * - Si el mes no existe, lo crea.
     * - Si el mes existe, lo devuelve.
     * - Si viene notes != null, actualiza notes.
     *
     * Esto funciona como "ensure budget month".
     */
    @Transactional
    public BudgetMonthResponse createBudgetMonth(
            UUID userId,
            UUID profileId,
            Integer year,
            BudgetMonthCreateRequest request
    ) {
        assertProfileOwner(profileId, userId);

        BudgetYear budgetYear = budgetYearRepository.findByProfileIdAndYear(profileId, year)
                .orElseGet(() -> budgetYearRepository.save(
                        BudgetYear.builder()
                                .profileId(profileId)
                                .year(year)
                                .build()
                ));

        Optional<BudgetMonth> existingMonth =
                budgetMonthRepository.findByBudgetYearIdAndMonth(
                        budgetYear.getId(),
                        request.month()
                );

        BudgetMonth budgetMonth = existingMonth.orElseGet(() ->
                BudgetMonth.builder()
                        .budgetYearId(budgetYear.getId())
                        .month(request.month())
                        .build()
        );

        if (existingMonth.isEmpty() || request.notes() != null) {
            budgetMonth.setNotes(request.notes());
        }

        return toBudgetMonthResponse(budgetMonthRepository.save(budgetMonth));
    }

    @Transactional(readOnly = true)
    public BudgetMonthResponse getBudgetMonth(
            UUID userId,
            UUID profileId,
            Integer year,
            Integer month
    ) {
        assertProfileOwner(profileId, userId);

        BudgetYear budgetYear = findBudgetYear(profileId, year);

        BudgetMonth budgetMonth = budgetMonthRepository
                .findByBudgetYearIdAndMonth(budgetYear.getId(), month)
                .orElseThrow(() -> new NotFoundException("Budget month not found"));

        return toBudgetMonthResponse(budgetMonth);
    }

    @Transactional
    public BudgetMonthResponse updateBudgetMonth(
            UUID userId,
            UUID budgetMonthId,
            BudgetMonthUpdateRequest request
    ) {
        BudgetMonth budgetMonth = budgetMonthRepository.findById(budgetMonthId)
                .orElseThrow(() -> new NotFoundException("Budget month not found"));

        BudgetYear budgetYear = budgetYearRepository.findById(budgetMonth.getBudgetYearId())
                .orElseThrow(() -> new NotFoundException("Budget year not found"));

        assertProfileOwner(budgetYear.getProfileId(), userId);

        budgetMonth.setNotes(request.notes());

        return toBudgetMonthResponse(budgetMonthRepository.save(budgetMonth));
    }

    @Transactional
    public BudgetCategoryItemResponse upsertBudgetCategoryItem(
            UUID userId,
            UUID budgetMonthId,
            BudgetCategoryItemUpsertRequest request
    ) {
        BudgetMonth budgetMonth = budgetMonthRepository.findById(budgetMonthId)
                .orElseThrow(() -> new NotFoundException("Budget month not found"));

        BudgetYear budgetYear = budgetYearRepository.findById(budgetMonth.getBudgetYearId())
                .orElseThrow(() -> new NotFoundException("Budget year not found"));

        assertProfileOwner(budgetYear.getProfileId(), userId);

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Category not found"));

        assertCategoryCanBeUsedInProfile(category, budgetYear.getProfileId());

        BudgetCategoryItem item = budgetCategoryItemRepository
                .findByBudgetMonthIdAndCategoryId(budgetMonthId, request.categoryId())
                .orElseGet(() ->
                        BudgetCategoryItem.builder()
                                .budgetMonthId(budgetMonthId)
                                .categoryId(request.categoryId())
                                .build()
                );

        item.setBudgetAmount(request.budgetAmount());

        return toBudgetCategoryItemResponse(
                budgetCategoryItemRepository.save(item),
                category
        );
    }

    @Transactional
    public void deleteBudgetCategoryItem(UUID userId, UUID itemId) {
        BudgetCategoryItem item = budgetCategoryItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Budget item not found"));

        BudgetMonth budgetMonth = budgetMonthRepository.findById(item.getBudgetMonthId())
                .orElseThrow(() -> new NotFoundException("Budget month not found"));

        BudgetYear budgetYear = budgetYearRepository.findById(budgetMonth.getBudgetYearId())
                .orElseThrow(() -> new NotFoundException("Budget year not found"));

        assertProfileOwner(budgetYear.getProfileId(), userId);

        budgetCategoryItemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public BudgetComparisonResponse getComparison(
            UUID userId,
            UUID profileId,
            Integer year,
            Integer month
    ) {
        assertProfileOwner(profileId, userId);

        BudgetMonth budgetMonth = findBudgetMonth(profileId, year, month);

        List<BudgetCategoryItem> budgetItems =
                budgetCategoryItemRepository.findByBudgetMonthId(budgetMonth.getId());

        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());

        List<MoneyTransaction> confirmedTransactions =
                transactionRepository.findByProfileIdAndBudgetDateBetween(profileId, from, to)
                        .stream()
                        .filter(this::isConfirmedBudgetCandidate)
                        .toList();

        HashSet<UUID> categoryIds = new HashSet<>();

        budgetItems.stream()
                .map(BudgetCategoryItem::getCategoryId)
                .filter(Objects::nonNull)
                .forEach(categoryIds::add);

        confirmedTransactions.stream()
                .map(MoneyTransaction::getCategoryId)
                .filter(Objects::nonNull)
                .forEach(categoryIds::add);

        var categoriesById = categoryRepository.findAllById(categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        var realAmountByCategoryId = confirmedTransactions.stream()
                .filter(transaction -> {
                    Category category = categoriesById.get(transaction.getCategoryId());
                    return isBudgetable(category);
                })
                .collect(Collectors.groupingBy(
                        MoneyTransaction::getCategoryId,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                MoneyTransaction::getAmount,
                                BigDecimal::add
                        )
                ));

        var budgetItemByCategoryId = budgetItems.stream()
                .filter(item -> {
                    Category category = categoriesById.get(item.getCategoryId());
                    return isBudgetable(category);
                })
                .collect(Collectors.toMap(
                        BudgetCategoryItem::getCategoryId,
                        Function.identity()
                ));

        HashSet<UUID> comparisonCategoryIds = new HashSet<>();

        comparisonCategoryIds.addAll(realAmountByCategoryId.keySet());
        comparisonCategoryIds.addAll(budgetItemByCategoryId.keySet());

        List<BudgetComparisonItemResponse> comparisonItems = new ArrayList<>();

        for (UUID categoryId : comparisonCategoryIds) {
            Category category = categoriesById.get(categoryId);

            BigDecimal budgetAmount = budgetItemByCategoryId.containsKey(categoryId)
                    ? budgetItemByCategoryId.get(categoryId).getBudgetAmount()
                    : BigDecimal.ZERO;

            BigDecimal realAmount = realAmountByCategoryId.getOrDefault(
                    categoryId,
                    BigDecimal.ZERO
            );

            BigDecimal difference = budgetAmount.subtract(realAmount);
            BigDecimal percentage = calculateUsagePercentage(budgetAmount, realAmount);
            BudgetStatus status = calculateBudgetStatus(budgetAmount, realAmount, percentage);

            comparisonItems.add(new BudgetComparisonItemResponse(
                    categoryId,
                    category == null ? "Unknown" : category.getName(),
                    category == null ? null : category.getType(),
                    budgetAmount,
                    realAmount,
                    difference,
                    percentage,
                    status
            ));
        }

        comparisonItems.sort(Comparator.comparing(BudgetComparisonItemResponse::categoryName));

        BigDecimal totalBudget = comparisonItems.stream()
                .map(BudgetComparisonItemResponse::budgetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReal = comparisonItems.stream()
                .map(BudgetComparisonItemResponse::realAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BudgetComparisonResponse(
                profileId,
                year,
                month,
                totalBudget,
                totalReal,
                totalBudget.subtract(totalReal),
                comparisonItems
        );
    }

    private void assertProfileOwner(UUID profileId, UUID userId) {
        financialProfileRepository.findByIdAndUserId(profileId, userId)
                .orElseThrow(() -> new ForbiddenException("Profile does not belong to user"));
    }

    private void assertCategoryCanBeUsedInProfile(Category category, UUID profileId) {
        if (category.getProfileId() != null && !Objects.equals(category.getProfileId(), profileId)) {
            throw new ForbiddenException("Category does not belong to profile");
        }
    }

    private BudgetYear findBudgetYear(UUID profileId, Integer year) {
        return budgetYearRepository.findByProfileIdAndYear(profileId, year)
                .orElseThrow(() -> new NotFoundException("Budget year not found"));
    }

    private BudgetMonth findBudgetMonth(UUID profileId, Integer year, Integer month) {
        BudgetYear budgetYear = findBudgetYear(profileId, year);

        return budgetMonthRepository.findByBudgetYearIdAndMonth(budgetYear.getId(), month)
                .orElseThrow(() -> new NotFoundException("Budget month not found"));
    }

    private boolean isBudgetable(Category.Type type) {
        return type == Category.Type.FIXED_EXPENSE
                || type == Category.Type.VARIABLE_EXPENSE
                || type == Category.Type.SAVING
                || type == Category.Type.DEBT
                || type == Category.Type.INVESTMENT;
    }

    private BigDecimal calculateUsagePercentage(
            BigDecimal budgetAmount,
            BigDecimal realAmount
    ) {
        if (budgetAmount.signum() == 0) {
            return realAmount.signum() == 0
                    ? BigDecimal.ZERO
                    : ONE_HUNDRED;
        }

        return realAmount
                .multiply(ONE_HUNDRED)
                .divide(budgetAmount, 2, RoundingMode.HALF_UP);
    }

    private BudgetStatus calculateBudgetStatus(
            BigDecimal budgetAmount,
            BigDecimal realAmount,
            BigDecimal percentage
    ) {
        if (budgetAmount.signum() == 0 && realAmount.signum() > 0) {
            return BudgetStatus.EXCEEDED;
        }

        if (percentage.compareTo(ONE_HUNDRED) > 0) {
            return BudgetStatus.EXCEEDED;
        }

        if (percentage.compareTo(WARNING_PERCENTAGE) > 0) {
            return BudgetStatus.WARNING;
        }

        return BudgetStatus.OK;
    }

    private BudgetYearResponse toBudgetYearResponse(BudgetYear budgetYear) {
        return new BudgetYearResponse(
                budgetYear.getId(),
                budgetYear.getProfileId(),
                budgetYear.getYear(),
                budgetYear.getTargetIncome(),
                budgetYear.getTargetSaving(),
                budgetYear.getNotes(),
                budgetYear.getCreatedAt(),
                budgetYear.getUpdatedAt()
        );
    }

    private BudgetMonthResponse toBudgetMonthResponse(BudgetMonth budgetMonth) {
        List<BudgetCategoryItem> items =
                budgetCategoryItemRepository.findByBudgetMonthId(budgetMonth.getId());

        var categoriesById = categoryRepository
                .findAllById(
                        items.stream()
                                .map(BudgetCategoryItem::getCategoryId)
                                .toList()
                )
                .stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<BudgetCategoryItemResponse> itemResponses = items.stream()
                .map(item -> {
                    Category category = categoriesById.get(item.getCategoryId());

                    if (category == null) {
                        throw new NotFoundException("Category not found for budget item");
                    }

                    return toBudgetCategoryItemResponse(item, category);
                })
                .toList();

        return new BudgetMonthResponse(
                budgetMonth.getId(),
                budgetMonth.getBudgetYearId(),
                budgetMonth.getMonth(),
                budgetMonth.getNotes(),
                itemResponses,
                budgetMonth.getCreatedAt(),
                budgetMonth.getUpdatedAt()
        );
    }

    private BudgetCategoryItemResponse toBudgetCategoryItemResponse(
            BudgetCategoryItem item,
            Category category
    ) {
        return new BudgetCategoryItemResponse(
                item.getId(),
                item.getBudgetMonthId(),
                item.getCategoryId(),
                category.getName(),
                category.getType(),
                item.getBudgetAmount(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private boolean isConfirmedBudgetCandidate(MoneyTransaction transaction) {
        if (transaction.getStatus() != MoneyTransaction.Status.CONFIRMED) {
            return false;
        }

        if (transaction.getCategoryId() == null) {
            return false;
        }

        if (transaction.getClassificationStatus() == MoneyTransaction.ClassificationStatus.NEEDS_CATEGORY
                || transaction.getClassificationStatus() == MoneyTransaction.ClassificationStatus.IGNORED_BY_RULE) {
            return false;
        }

        if (transaction.getMovementType() == MoneyTransaction.MovementType.TRANSFER) {
            return false;
        }

        return transaction.getMovementType() == MoneyTransaction.MovementType.EXPENSE
                || transaction.getMovementType() == MoneyTransaction.MovementType.SAVING
                || transaction.getMovementType() == MoneyTransaction.MovementType.ADJUSTMENT;
    }

    private boolean isBudgetable(Category category) {
        return category != null
                && Boolean.TRUE.equals(category.getActive())
                && Boolean.TRUE.equals(category.getBudgetable())
                && !Boolean.TRUE.equals(category.getTechnical())
                && isBudgetable(category.getType());
    }
}