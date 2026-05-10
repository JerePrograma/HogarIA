import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
// src/features/budgets/BudgetPage.tsx
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createBudgetMonth, createBudgetYear, getBudgetComparison, getBudgetMonth, getBudgetYear, upsertBudgetCategoryItem, } from '../../api/budgetsApi';
import { listCategories } from '../../api/categoriesApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { budgetComparisonStatusLabels, categoryTypeLabels, labelOrValue, monthLabels, } from '../../domain/financeLabels';
import { formatMoney, formatPercent } from '../../domain/formatters';
export function BudgetPage() {
    const { profileId = '' } = useParams();
    const qc = useQueryClient();
    const today = new Date();
    const [year, setYear] = useState(today.getFullYear());
    const [month, setMonth] = useState(today.getMonth() + 1);
    const budgetYearQuery = useQuery({
        queryKey: ['budget-year', profileId, year],
        queryFn: () => getBudgetYear(profileId, year),
        retry: false,
        enabled: Boolean(profileId),
    });
    const budgetMonthQuery = useQuery({
        queryKey: ['budget-month', profileId, year, month],
        queryFn: () => getBudgetMonth(profileId, year, month),
        retry: false,
        enabled: Boolean(profileId),
    });
    const comparisonQuery = useQuery({
        queryKey: ['budget-comp', profileId, year, month],
        queryFn: () => getBudgetComparison(profileId, year, month),
        retry: false,
        enabled: Boolean(profileId),
    });
    const categoriesQuery = useQuery({
        queryKey: ['categories', profileId],
        queryFn: () => listCategories(profileId, true),
        enabled: Boolean(profileId),
    });
    const budgetableCategories = (categoriesQuery.data ?? []).filter((category) => category.active);
    const createYearMutation = useMutation({
        mutationFn: () => createBudgetYear(profileId, { year }),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['budget-year', profileId, year] });
        },
    });
    const createMonthMutation = useMutation({
        mutationFn: () => createBudgetMonth(profileId, year, { month }),
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['budget-month', profileId, year, month] });
        },
    });
    const saveItemMutation = useMutation({
        mutationFn: ({ categoryId, budgetAmount, }) => {
            if (!budgetMonthQuery.data?.id) {
                throw new Error('Primero tenés que crear el mes presupuestario.');
            }
            return upsertBudgetCategoryItem(budgetMonthQuery.data.id, {
                categoryId,
                budgetAmount,
            });
        },
        onSuccess: () => {
            qc.invalidateQueries({ queryKey: ['budget-month', profileId, year, month] });
            qc.invalidateQueries({ queryKey: ['budget-comp', profileId, year, month] });
        },
    });
    const findBudgetAmount = (categoryId) => {
        const item = budgetMonthQuery.data?.items?.find((i) => i.categoryId === categoryId);
        return item?.budgetAmount ?? 0;
    };
    return (_jsx(AppLayout, { children: _jsxs("div", { className: "card", children: [_jsx("h1", { children: "Presupuesto" }), _jsxs("div", { className: "form-row", children: [_jsx("input", { className: "input", type: "number", value: year, min: 2000, max: 2100, onChange: (e) => setYear(Number(e.target.value)) }), _jsx("select", { className: "select", value: month, onChange: (e) => setMonth(Number(e.target.value)), children: Object.entries(monthLabels).map(([value, label]) => (_jsx("option", { value: value, children: label }, value))) }), _jsx("button", { onClick: () => createYearMutation.mutate(), disabled: createYearMutation.isPending, children: "Crear a\u00F1o" }), _jsx("button", { onClick: () => createMonthMutation.mutate(), disabled: createMonthMutation.isPending, children: "Crear mes" })] }), _jsxs("p", { children: ["A\u00F1o: ", budgetYearQuery.data ? 'OK' : 'No existe', " | Mes:", ' ', budgetMonthQuery.data ? 'OK' : 'No existe'] }), !budgetMonthQuery.data && (_jsx("p", { className: "empty-state", children: "Primero cre\u00E1 el mes para poder cargar importes presupuestados." })), _jsx("h3", { children: "Carga presupuestaria" }), _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Categor\u00EDa" }), _jsx("th", { children: "Tipo" }), _jsx("th", { children: "Presupuesto" })] }) }), _jsx("tbody", { children: budgetableCategories.map((category) => (_jsxs("tr", { children: [_jsx("td", { children: category.name }), _jsx("td", { children: labelOrValue(categoryTypeLabels, category.type) }), _jsx("td", { children: _jsx("input", { className: "input", type: "number", min: 0, defaultValue: findBudgetAmount(category.id), disabled: !budgetMonthQuery.data, onBlur: (e) => budgetMonthQuery.data &&
                                                saveItemMutation.mutate({
                                                    categoryId: category.id,
                                                    budgetAmount: Number(e.target.value),
                                                }) }) })] }, category.id))) })] }), _jsx("h3", { children: "Presupuesto vs Real" }), _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Categor\u00EDa" }), _jsx("th", { children: "Tipo" }), _jsx("th", { children: "Presupuesto" }), _jsx("th", { children: "Real" }), _jsx("th", { children: "Diferencia" }), _jsx("th", { children: "% usado" }), _jsx("th", { children: "Estado" })] }) }), _jsx("tbody", { children: comparisonQuery.data?.items?.map((item) => (_jsxs("tr", { children: [_jsx("td", { children: item.categoryName }), _jsx("td", { children: labelOrValue(categoryTypeLabels, item.categoryType) }), _jsx("td", { children: formatMoney(item.budgetAmount) }), _jsx("td", { children: formatMoney(item.realAmount) }), _jsx("td", { children: formatMoney(item.difference) }), _jsx("td", { children: formatPercent(item.percentUsed) }), _jsx("td", { children: _jsx("span", { className: `badge ${item.status === 'EXCEEDED'
                                                ? 'badge-danger'
                                                : item.status === 'WARNING'
                                                    ? 'badge-warning'
                                                    : 'badge-ok'}`, children: labelOrValue(budgetComparisonStatusLabels, item.status) }) })] }, item.categoryId))) })] }), _jsxs("p", { children: ["Totales: Presupuesto", ' ', formatMoney(comparisonQuery.data?.totalBudget), " | Real", ' ', formatMoney(comparisonQuery.data?.totalReal), " | Diferencia", ' ', formatMoney(comparisonQuery.data?.totalDifference)] })] }) }));
}
