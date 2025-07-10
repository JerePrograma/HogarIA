package com.hogaria.reports.service.impl;

import com.hogaria.expense.dto.ExpenseDto;
import com.hogaria.expense.model.Category;
import com.hogaria.expense.repository.ExpenseRepository;
import com.hogaria.expense.repository.CategoryRepository;
import com.hogaria.reports.dto.*;
import com.hogaria.reports.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ExpenseRepository expenseRepo;
    private final CategoryRepository categoryRepo;

    public ReportServiceImpl(ExpenseRepository expenseRepo,
                             CategoryRepository categoryRepo) {
        this.expenseRepo = expenseRepo;
        this.categoryRepo = categoryRepo;
    }

    @Override
    public ReportSummaryDto summarize(Long familyId, LocalDate startDate, LocalDate endDate) {
        List<ExpenseDto> expenses = expenseRepo
                .findAllByFilters(familyId, startDate, endDate, null, null)
                .stream().map(e -> new ExpenseDto(
                        e.getId(), e.getFamilyId(), e.getUserId(), e.getAmount(),
                        e.getCurrency(), e.getCategoryId(), e.getSubcategoryId(),
                        e.getDescripcion(), e.getDate()
                )).toList();

        double totalExpenses = expenses.stream()
                .mapToDouble(e -> e.amount().doubleValue())
                .sum();

        // No hay tabla de ingresos, asumimos cero
        double totalIncome = 0.0;

        // Agrupar por categoria
        Map<Long, Double> sumByCatId = new HashMap<>();
        for (ExpenseDto e : expenses) {
            long catId = Optional.ofNullable(e.categoryId()).orElse(-1L);
            sumByCatId.merge(catId, e.amount().doubleValue(), Double::sum);
        }

        List<CategoryAmountDto> byCategory = sumByCatId.entrySet().stream()
                .map(entry -> {
                    String name = entry.getKey() < 0
                            ? "Uncategorized"
                            : categoryRepo.findById(entry.getKey())
                            .map(Category::getNombre)
                            .orElse("Unknown");
                    return new CategoryAmountDto(name, entry.getValue());
                })
                .collect(Collectors.toList());

        return new ReportSummaryDto(totalExpenses, totalIncome, byCategory);
    }

    @Override
    public ForecastDto forecast(Long familyId, int horizonDays) {
        // Tomamos los ultimos 30 dias para calcular el promedio diario
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(30);
        List<ExpenseDto> pastExpenses = expenseRepo
                .findAllByFilters(familyId, from, today, null, null)
                .stream().map(e -> new ExpenseDto(
                        e.getId(), e.getFamilyId(), e.getUserId(), e.getAmount(),
                        e.getCurrency(), e.getCategoryId(), e.getSubcategoryId(),
                        e.getDescripcion(), e.getDate()
                )).toList();

        long days = ChronoUnit.DAYS.between(from, today) + 1;
        double total = pastExpenses.stream()
                .mapToDouble(e -> e.amount().doubleValue())
                .sum();
        double avgPerDay = days > 0 ? total / days : 0.0;

        List<ForecastEntryDto> projected = new ArrayList<>();
        for (int i = 1; i <= horizonDays; i++) {
            LocalDate d = today.plusDays(i);
            projected.add(new ForecastEntryDto(d, avgPerDay));
        }

        String advice;
        if (avgPerDay * horizonDays > total * 0.5) {
            advice = "Tus gastos proyectados son altos; considera reducir consumos.";
        } else {
            advice = "Proyeccion dentro de un rango razonable.";
        }

        return new ForecastDto(projected, advice);
    }
}
