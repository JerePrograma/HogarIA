export const budgetQueryKeys = {
  year: (profileId: string, year: number) => ["budget-year", profileId, year],
  month: (profileId: string, year: number, month: number) => [
    "budget-month",
    profileId,
    year,
    month,
  ],
  comparison: (profileId: string, year: number, month: number) => [
    "budget-comp",
    profileId,
    year,
    month,
  ],
  categories: (profileId: string) => ["categories", profileId],
};
