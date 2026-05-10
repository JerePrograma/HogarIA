// src/domain/financeOptions.ts
import { accountTypeLabels, categoryScopeLabels, categoryTypeLabels, movementTypeLabels, profileTypeLabels, transactionStatusLabels, monthlyPlanTypeLabels, monthlyPlanPriorityLabels, monthlyPlanStatusLabels, monthlyPlanSourceLabels, } from './financeLabels';
function toOptions(labels) {
    return Object.entries(labels).map(([value, label]) => ({
        value: value,
        label: label,
    }));
}
export const profileTypeOptions = toOptions(profileTypeLabels);
export const accountTypeOptions = toOptions(accountTypeLabels);
export const categoryTypeOptions = toOptions(categoryTypeLabels);
export const categoryScopeOptions = toOptions(categoryScopeLabels);
export const movementTypeOptions = toOptions(movementTypeLabels);
export const transactionStatusOptions = toOptions(transactionStatusLabels);
export const monthlyPlanTypeOptions = toOptions(monthlyPlanTypeLabels);
export const monthlyPlanPriorityOptions = toOptions(monthlyPlanPriorityLabels);
export const monthlyPlanStatusOptions = toOptions(monthlyPlanStatusLabels);
export const monthlyPlanSourceOptions = toOptions(monthlyPlanSourceLabels);
