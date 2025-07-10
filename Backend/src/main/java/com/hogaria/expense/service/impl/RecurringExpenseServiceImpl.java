package com.hogaria.expense.service.impl;

import com.hogaria.expense.dto.RecurringExpenseDto;
import com.hogaria.expense.model.Expense;
import com.hogaria.expense.model.RecurringExpense;
import com.hogaria.expense.model.RecurringFrequencyEnum;
import com.hogaria.expense.repository.ExpenseRepository;
import com.hogaria.expense.repository.RecurringExpenseRepository;
import com.hogaria.expense.service.RecurringExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;

@Service
@Transactional
public class RecurringExpenseServiceImpl implements RecurringExpenseService {

    private final RecurringExpenseRepository repo;
    private final ExpenseRepository expenseRepo;
    private final RecurringExpenseRepository recurringExpenseRepository;

    @Autowired
    public RecurringExpenseServiceImpl(RecurringExpenseRepository repo, ExpenseRepository expenseRepo, RecurringExpenseRepository recurringExpenseRepository) {
        this.repo = repo;
        this.expenseRepo = expenseRepo;
        this.recurringExpenseRepository = recurringExpenseRepository;
    }

    private static RecurringExpenseDto toDto(RecurringExpense e) {
        return new RecurringExpenseDto(
                e.getId(),
                e.getExpenseTemplate().getId(),
                e.getFrequency().name(),
                e.getNextDueDate()
        );
    }

    private RecurringExpense toEntity(RecurringExpenseDto d) {
        RecurringExpense e = new RecurringExpense();
        e.setExpenseTemplate(new RecurringExpense().getExpenseTemplate()); // ajusta segun fetch real
        e.setFrequency(RecurringFrequencyEnum.valueOf(d.frequency()));
        e.setNextDueDate(d.nextDueDate());
        return e;
    }

    @Override
    public List<RecurringExpenseDto> getByFamilyId(Long familyId) {
        return repo.findAllByExpenseTemplate_FamilyId(familyId)
                .stream().map(RecurringExpenseServiceImpl::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public RecurringExpenseDto create(RecurringExpenseDto dto) {
        Expense template = expenseRepo.findById(dto.expenseTemplateId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Expense template not found"));

        RecurringExpense e = new RecurringExpense();
        e.setExpenseTemplate(template);
        e.setFrequency(RecurringFrequencyEnum.valueOf(dto.frequency()));
        e.setNextDueDate(dto.nextDueDate());

        return toDto(recurringExpenseRepository.save(e));
    }

    @Override
    public RecurringExpenseDto update(Long id, RecurringExpenseDto dto) {
        RecurringExpense existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No existe"));
        existing.setFrequency(RecurringFrequencyEnum.valueOf(dto.frequency()));
        existing.setNextDueDate(dto.nextDueDate());
        return toDto(repo.save(existing));
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "No existe");
        }
        repo.deleteById(id);
    }
}
