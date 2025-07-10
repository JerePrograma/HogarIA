package com.hogaria.expense.service.impl;

import com.hogaria.expense.dto.ExpenseDto;
import com.hogaria.expense.model.Expense;
import com.hogaria.expense.repository.ExpenseRepository;
import com.hogaria.expense.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository repo;

    @Autowired
    public ExpenseServiceImpl(ExpenseRepository repo) {
        this.repo = repo;
    }

    private static ExpenseDto toDto(Expense e) {
        return new ExpenseDto(
                e.getId(),
                e.getFamilyId(),
                e.getUserId(),
                e.getAmount(),
                e.getCurrency(),
                e.getCategoryId(),
                e.getSubcategoryId(),
                e.getDescripcion(),
                e.getDate()
        );
    }

    private static Expense toEntity(ExpenseDto d) {
        Expense e = new Expense();
        e.setFamilyId(d.familyId());
        e.setUserId(d.userId());
        e.setAmount(d.amount());
        e.setCurrency(d.currency());
        e.setCategoryId(d.categoryId());
        e.setSubcategoryId(d.subcategoryId());
        e.setDescripcion(d.descripcion());
        e.setDate(d.date());
        return e;
    }

    @Override
    public ExpenseDto createExpense(ExpenseDto dto) {
        Expense saved = repo.save(toEntity(dto));
        return toDto(saved);
    }

    @Override
    public ExpenseDto updateExpense(Long id, ExpenseDto dto) {
        Expense existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
        existing.setFamilyId(dto.familyId());
        existing.setUserId(dto.userId());
        existing.setAmount(dto.amount());
        existing.setCurrency(dto.currency());
        existing.setCategoryId(dto.categoryId());
        existing.setSubcategoryId(dto.subcategoryId());
        existing.setDescripcion(dto.descripcion());
        existing.setDate(dto.date());
        Expense updated = repo.save(existing);
        return toDto(updated);
    }

    @Override
    public List<ExpenseDto> getExpenses(Long familyId, LocalDate startDate,
                                        LocalDate endDate, Long categoryId, Long subcategoryId) {
        return repo.findAllByFilters(familyId, startDate, endDate, categoryId, subcategoryId)
                .stream()
                .map(ExpenseServiceImpl::toDto)
                .collect(Collectors.toList());
    }
}
