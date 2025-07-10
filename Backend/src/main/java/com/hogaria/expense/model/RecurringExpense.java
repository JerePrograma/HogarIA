package com.hogaria.expense.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "recurring_expenses")
@Setter
public class RecurringExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Enlaza con la plantilla de gasto
    @ManyToOne
    @JoinColumn(name = "expense_template_id", nullable = false)
    private Expense expenseTemplate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurringFrequencyEnum frequency;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    public RecurringExpense() {
    }

}
