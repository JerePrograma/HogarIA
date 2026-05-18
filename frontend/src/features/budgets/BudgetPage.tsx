import { useState } from "react";
import { useParams } from "react-router-dom";
import { AppLayout } from "../../components/layout/AppLayout";
import { ErrorState } from "../../components/ui/ErrorState";
import { useBudgetPeriodActions } from "./hooks/useBudgetPeriodActions";
import { useBudgetWorkspace } from "./hooks/useBudgetWorkspace";
import { BudgetPageHeader } from "./components/BudgetPageHeader";
import { BudgetComparisonPanel } from "./components/BudgetComparisonPanel";
import { BudgetEditorPanel } from "./components/BudgetEditorPanel";
import { BudgetSetupPanel } from "./components/BudgetSetupPanel";
import { BudgetSummaryMetrics } from "./components/BudgetSummaryMetrics";
import { BudgetFeedback } from "./components/BudgetFeedback";

export function BudgetPage() {
  const { profileId = "" } = useParams();

  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);

  const workspace = useBudgetWorkspace(profileId, year, month);

  const actions = useBudgetPeriodActions({
    profileId,
    year,
    month,
    budgetMonthId: workspace.budgetMonthQuery.data?.id,
  });

  if (!profileId) {
    return (
      <AppLayout>
        <ErrorState message="No hay un perfil financiero seleccionado." />
      </AppLayout>
    );
  }

  const savingCategoryId = actions.saveItemMutation.isPending
    ? actions.saveItemMutation.variables?.categoryId
    : null;

  return (
    <AppLayout>
      <div className="page-stack">
        <BudgetPageHeader
          year={year}
          month={month}
          onYearChange={setYear}
          onMonthChange={setMonth}
        />

        <BudgetFeedback
          budgetYearQuery={workspace.budgetYearQuery}
          budgetMonthQuery={workspace.budgetMonthQuery}
          comparisonQuery={workspace.comparisonQuery}
          preparePeriodMutation={actions.preparePeriodMutation}
          budgetYearMissing={workspace.budgetYearMissing}
          budgetMonthMissing={workspace.budgetMonthMissing}
        />

        {!workspace.hasBudgetMonth ? (
          <BudgetSetupPanel
            year={year}
            month={month}
            loading={
              workspace.budgetYearQuery.isLoading ||
              workspace.budgetMonthQuery.isLoading
            }
            pending={actions.preparePeriodMutation.isPending}
            onPreparePeriod={() => actions.preparePeriodMutation.mutate()}
          />
        ) : (
          <>
            <BudgetSummaryMetrics comparison={workspace.comparisonQuery.data} />

            <BudgetEditorPanel
              categories={workspace.budgetableCategories}
              loading={workspace.categoriesQuery.isLoading}
              error={workspace.categoriesQuery.isError}
              amountByCategoryId={workspace.budgetAmountByCategoryId}
              canEdit={workspace.canEditBudget}
              savingCategoryId={savingCategoryId}
              saveError={actions.saveItemMutation.error}
              onSaveAmount={(categoryId, budgetAmount) =>
                actions.saveItemMutation.mutate({
                  categoryId,
                  budgetAmount,
                })
              }
            />

            <BudgetComparisonPanel
              budgetMonthExists={workspace.hasBudgetMonth}
              comparisonQuery={workspace.comparisonQuery}
            />
          </>
        )}
      </div>
    </AppLayout>
  );
}
