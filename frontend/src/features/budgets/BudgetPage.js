import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { createBudgetMonth, createBudgetYear, getBudgetComparison, getBudgetMonth, getBudgetYear, upsertBudgetCategoryItem, } from '../../api/budgetsApi';
import { listCategories } from '../../api/categoriesApi';
import { AppLayout } from '../../components/layout/AppLayout';
import { budgetComparisonStatusLabels, categoryTypeLabels, labelOrMissing, monthLabels, } from '../../domain/financeLabels';
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
        mutationFn: ({ categoryId, budgetAmount }) => {
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
    const comparison = comparisonQuery.data;
    return (_jsxs(AppLayout, { children: [_jsxs("div", { className: "page-header", children: [_jsxs("div", { children: [_jsx("p", { className: "eyebrow", children: "Planificaci\u00F3n" }), _jsx("h1", { children: "Presupuesto" }), _jsx("p", { className: "muted", children: "Defin\u00ED importes esperados y comparalos contra la ejecuci\u00F3n real." })] }), _jsxs("div", { className: "toolbar", children: [_jsxs("label", { className: "field compact-field", children: [_jsx("span", { children: "A\u00F1o" }), _jsx("input", { type: "number", value: year, onChange: (event) => setYear(Number(event.target.value)) })] }), _jsxs("label", { className: "field compact-field", children: [_jsx("span", { children: "Mes" }), _jsx("select", { value: month, onChange: (event) => setMonth(Number(event.target.value)), children: Object.entries(monthLabels).map(([value, label]) => (_jsx("option", { value: value, children: label }, value))) })] })] })] }), _jsxs("section", { className: "card", children: [_jsxs("div", { className: "status-row", children: [_jsxs("span", { className: `badge ${budgetYearQuery.data ? 'good' : 'warning'}`, children: ["A\u00F1o: ", budgetYearQuery.data ? 'Creado' : 'No creado'] }), _jsxs("span", { className: `badge ${budgetMonthQuery.data ? 'good' : 'warning'}`, children: ["Mes: ", budgetMonthQuery.data ? 'Creado' : 'No creado'] })] }), _jsxs("div", { className: "actions", children: [_jsx("button", { type: "button", className: "button secondary", onClick: () => createYearMutation.mutate(), disabled: createYearMutation.isPending || Boolean(budgetYearQuery.data), children: "Crear a\u00F1o" }), _jsx("button", { type: "button", className: "button secondary", onClick: () => createMonthMutation.mutate(), disabled: createMonthMutation.isPending || Boolean(budgetMonthQuery.data), children: "Crear mes" })] }), !budgetMonthQuery.data && (_jsx("div", { className: "alert warning", children: "Primero cre\u00E1 el mes para poder cargar importes presupuestados." }))] }), _jsxs("section", { className: "card", children: [_jsx("h2", { children: "Carga presupuestaria" }), !budgetableCategories.length ? (_jsx("p", { className: "muted", children: "No hay categor\u00EDas activas para presupuestar." })) : (_jsx("div", { className: "table-wrap", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Categor\u00EDa" }), _jsx("th", { children: "Tipo" }), _jsx("th", { className: "amount", children: "Presupuesto" })] }) }), _jsx("tbody", { children: budgetableCategories.map((category) => (_jsxs("tr", { children: [_jsx("td", { children: category.name }), _jsx("td", { children: labelOrMissing(categoryTypeLabels, category.type) }), _jsx("td", { className: "amount", children: _jsx("input", { className: "amount-input", type: "number", min: "0", defaultValue: findBudgetAmount(category.id), disabled: !budgetMonthQuery.data, onBlur: (event) => budgetMonthQuery.data &&
                                                        saveItemMutation.mutate({
                                                            categoryId: category.id,
                                                            budgetAmount: Number(event.currentTarget.value),
                                                        }) }) })] }, category.id))) })] }) }))] }), _jsxs("section", { className: "card", children: [_jsx("h2", { children: "Presupuesto vs ejecutado" }), !comparison?.items?.length ? (_jsx("p", { className: "muted", children: "No hay datos comparativos para este per\u00EDodo." })) : (_jsxs(_Fragment, { children: [_jsx("div", { className: "table-wrap", children: _jsxs("table", { className: "table", children: [_jsx("thead", { children: _jsxs("tr", { children: [_jsx("th", { children: "Categor\u00EDa" }), _jsx("th", { children: "Tipo" }), _jsx("th", { className: "amount", children: "Presupuesto" }), _jsx("th", { className: "amount", children: "Ejecutado" }), _jsx("th", { className: "amount", children: "Diferencia" }), _jsx("th", { className: "amount", children: "% usado" }), _jsx("th", { children: "Estado" })] }) }), _jsx("tbody", { children: comparison.items.map((item) => (_jsxs("tr", { children: [_jsx("td", { children: item.categoryName }), _jsx("td", { children: labelOrMissing(categoryTypeLabels, item.categoryType) }), _jsx("td", { className: "amount", children: formatMoney(item.budgetAmount) }), _jsx("td", { className: "amount", children: formatMoney(item.realAmount) }), _jsx("td", { className: "amount", children: formatMoney(item.difference) }), _jsx("td", { className: "amount", children: formatPercent(item.percentUsed) }), _jsx("td", { children: _jsx("span", { className: `badge ${item.status === 'EXCEEDED' ? 'danger' : item.status === 'WARNING' ? 'warning' : 'good'}`, children: labelOrMissing(budgetComparisonStatusLabels, item.status) }) })] }, item.categoryId))) })] }) }), _jsxs("p", { className: "summary-line", children: ["Totales: Presupuesto ", formatMoney(comparison.totalBudget), " | Ejecutado ", formatMoney(comparison.totalReal), " | Diferencia ", formatMoney(comparison.totalDifference)] })] }))] })] }));
}
