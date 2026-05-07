package com.hogaria.controller;
import com.hogaria.dto.BudgetDtos.*;import com.hogaria.security.CurrentUserResolver;import com.hogaria.service.BudgetService;import jakarta.validation.Valid;import java.util.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class BudgetController {
private final BudgetService s; private final CurrentUserResolver cu;
public BudgetController(BudgetService s,CurrentUserResolver cu){this.s=s;this.cu=cu;}
@PostMapping("/profiles/{profileId}/budgets") public BudgetYearResponse createBudgetYear(@RequestHeader("X-User-Id") String x,@PathVariable UUID profileId,@Valid @RequestBody BudgetYearCreateRequest r){return s.createBudgetYear(cu.parse(x),profileId,r);} 
@GetMapping("/profiles/{profileId}/budgets") public List<BudgetYearResponse> listBudgetYears(@RequestHeader("X-User-Id") String x,@PathVariable UUID profileId){return s.listBudgetYears(cu.parse(x),profileId);} 
@GetMapping("/profiles/{profileId}/budgets/{year}") public BudgetYearResponse getBudgetYear(@RequestHeader("X-User-Id") String x,@PathVariable UUID profileId,@PathVariable Integer year){return s.getBudgetYear(cu.parse(x),profileId,year);} 
@PutMapping("/profiles/{profileId}/budgets/{year}") public BudgetYearResponse updateBudgetYear(@RequestHeader("X-User-Id") String x,@PathVariable UUID profileId,@PathVariable Integer year,@Valid @RequestBody BudgetYearUpdateRequest r){return s.updateBudgetYear(cu.parse(x),profileId,year,r);} 
@PostMapping("/profiles/{profileId}/budgets/{year}/months") public BudgetMonthResponse createBudgetMonth(@RequestHeader("X-User-Id") String x,@PathVariable UUID profileId,@PathVariable Integer year,@Valid @RequestBody BudgetMonthCreateRequest r){return s.createBudgetMonth(cu.parse(x),profileId,year,r);} 
@GetMapping("/profiles/{profileId}/budgets/{year}/months/{month}") public BudgetMonthResponse getBudgetMonth(@RequestHeader("X-User-Id") String x,@PathVariable UUID profileId,@PathVariable Integer year,@PathVariable Integer month){return s.getBudgetMonth(cu.parse(x),profileId,year,month);} 
@PutMapping("/budget-months/{budgetMonthId}") public BudgetMonthResponse updateBudgetMonth(@RequestHeader("X-User-Id") String x,@PathVariable UUID budgetMonthId,@Valid @RequestBody BudgetMonthUpdateRequest r){return s.updateBudgetMonth(cu.parse(x),budgetMonthId,r);} 
@PutMapping("/budget-months/{budgetMonthId}/items") public BudgetCategoryItemResponse upsertBudgetCategoryItem(@RequestHeader("X-User-Id") String x,@PathVariable UUID budgetMonthId,@Valid @RequestBody BudgetCategoryItemUpsertRequest r){return s.upsertBudgetCategoryItem(cu.parse(x),budgetMonthId,r);} 
@DeleteMapping("/budget-category-items/{itemId}") public void deleteBudgetCategoryItem(@RequestHeader("X-User-Id") String x,@PathVariable UUID itemId){s.deleteBudgetCategoryItem(cu.parse(x),itemId);} 
@GetMapping("/profiles/{profileId}/budgets/{year}/months/{month}/comparison") public BudgetComparisonResponse getComparison(@RequestHeader("X-User-Id") String x,@PathVariable UUID profileId,@PathVariable Integer year,@PathVariable Integer month){return s.getComparison(cu.parse(x),profileId,year,month);} }
