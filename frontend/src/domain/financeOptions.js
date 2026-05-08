import { accountTypeLabels, categoryScopeLabels, categoryTypeLabels, goalTypeLabels, habitFrequencyLabels, movementTypeLabels, profileTypeLabels, transactionStatusLabels, } from './financeLabels';
function toOptions(labels) {
    return Object.entries(labels).map(([value, label]) => ({
        value: value,
        label: String(label),
    }));
}
export const profileTypeOptions = toOptions(profileTypeLabels);
export const accountTypeOptions = toOptions(accountTypeLabels);
export const categoryTypeOptions = toOptions(categoryTypeLabels);
export const categoryScopeOptions = toOptions(categoryScopeLabels);
export const movementTypeOptions = toOptions(movementTypeLabels);
export const transactionStatusOptions = toOptions(transactionStatusLabels);
export const goalTypeOptions = toOptions(goalTypeLabels);
export const habitFrequencyOptions = toOptions(habitFrequencyLabels);
