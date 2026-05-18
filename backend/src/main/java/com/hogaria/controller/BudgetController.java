package com.hogaria.controller;

import com.hogaria.dto.BudgetDtos.BudgetCategoryItemResponse;
import com.hogaria.dto.BudgetDtos.BudgetCategoryItemUpsertRequest;
import com.hogaria.dto.BudgetDtos.BudgetComparisonResponse;
import com.hogaria.dto.BudgetDtos.BudgetMonthCreateRequest;
import com.hogaria.dto.BudgetDtos.BudgetMonthResponse;
import com.hogaria.dto.BudgetDtos.BudgetMonthUpdateRequest;
import com.hogaria.dto.BudgetDtos.BudgetYearCreateRequest;
import com.hogaria.dto.BudgetDtos.BudgetYearResponse;
import com.hogaria.dto.BudgetDtos.BudgetYearUpdateRequest;
import com.hogaria.security.CurrentUserResolver;
import com.hogaria.service.BudgetService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BudgetController {

    private final BudgetService budgetService;
    private final CurrentUserResolver currentUserResolver;

    public BudgetController(
            BudgetService budgetService,
            CurrentUserResolver currentUserResolver
    ) {
        this.budgetService = budgetService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/profiles/{profileId}/budgets")
    public BudgetYearResponse createBudgetYear(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID profileId,
            @Valid @RequestBody BudgetYearCreateRequest request
    ) {
        return budgetService.createBudgetYear(
                currentUserResolver.parse(userHeader),
                profileId,
                request
        );
    }

    @GetMapping("/profiles/{profileId}/budgets")
    public List<BudgetYearResponse> listBudgetYears(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID profileId
    ) {
        return budgetService.listBudgetYears(
                currentUserResolver.parse(userHeader),
                profileId
        );
    }

    @GetMapping("/profiles/{profileId}/budgets/{year}")
    public BudgetYearResponse getBudgetYear(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID profileId,
            @PathVariable Integer year
    ) {
        return budgetService.getBudgetYear(
                currentUserResolver.parse(userHeader),
                profileId,
                year
        );
    }

    @PutMapping("/profiles/{profileId}/budgets/{year}")
    public BudgetYearResponse updateBudgetYear(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID profileId,
            @PathVariable Integer year,
            @Valid @RequestBody BudgetYearUpdateRequest request
    ) {
        return budgetService.updateBudgetYear(
                currentUserResolver.parse(userHeader),
                profileId,
                year,
                request
        );
    }

    @PostMapping("/profiles/{profileId}/budgets/{year}/months")
    public BudgetMonthResponse createBudgetMonth(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID profileId,
            @PathVariable Integer year,
            @Valid @RequestBody BudgetMonthCreateRequest request
    ) {
        return budgetService.createBudgetMonth(
                currentUserResolver.parse(userHeader),
                profileId,
                year,
                request
        );
    }

    @GetMapping("/profiles/{profileId}/budgets/{year}/months/{month}")
    public BudgetMonthResponse getBudgetMonth(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID profileId,
            @PathVariable Integer year,
            @PathVariable Integer month
    ) {
        return budgetService.getBudgetMonth(
                currentUserResolver.parse(userHeader),
                profileId,
                year,
                month
        );
    }

    @PutMapping("/budget-months/{budgetMonthId}")
    public BudgetMonthResponse updateBudgetMonth(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID budgetMonthId,
            @Valid @RequestBody BudgetMonthUpdateRequest request
    ) {
        return budgetService.updateBudgetMonth(
                currentUserResolver.parse(userHeader),
                budgetMonthId,
                request
        );
    }

    @PutMapping("/budget-months/{budgetMonthId}/items")
    public BudgetCategoryItemResponse upsertBudgetCategoryItem(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID budgetMonthId,
            @Valid @RequestBody BudgetCategoryItemUpsertRequest request
    ) {
        return budgetService.upsertBudgetCategoryItem(
                currentUserResolver.parse(userHeader),
                budgetMonthId,
                request
        );
    }

    @DeleteMapping("/budget-category-items/{itemId}")
    public void deleteBudgetCategoryItem(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID itemId
    ) {
        budgetService.deleteBudgetCategoryItem(
                currentUserResolver.parse(userHeader),
                itemId
        );
    }

    @GetMapping("/profiles/{profileId}/budgets/{year}/months/{month}/comparison")
    public BudgetComparisonResponse getComparison(
            @RequestHeader("X-User-Id") String userHeader,
            @PathVariable UUID profileId,
            @PathVariable Integer year,
            @PathVariable Integer month
    ) {
        return budgetService.getComparison(
                currentUserResolver.parse(userHeader),
                profileId,
                year,
                month
        );
    }
}