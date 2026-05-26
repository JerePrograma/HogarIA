import { AccountsPage } from "../../domains/accounts/AccountsPage";
import { BudgetPage } from "../../domains/budgets/BudgetPage";
import { CategoriesPage } from "../../domains/categories/CategoriesPage";
import { DashboardPage } from "../../domains/dashboard/DashboardPage";
import { DevUserPage } from "../../domains/dev-user/DevUserPage";
import { ExternalLoansPage } from "../../domains/external-loans/ExternalLoansPage";
import { GoalsPage } from "../../domains/goals/GoalsPage";
import { HabitsPage } from "../../domains/habits/HabitsPage";
import { InflationPage } from "../../domains/inflation/InflationPage";
import { MonthlyPlanReconciliationPage } from "../../domains/monthly-planning/MonthlyPlanReconciliationPage";
import {
  BudgetPlanningSuggestionsPage,
  LegacyPlanningRedirect,
  MonthlyPlanAlertsPage,
  MonthlyPlanConvertPage,
  MonthlyPlanImportPage,
  MonthlyPlanItemCreatePage,
  MonthlyPlanItemEditPage,
  MonthlyPlanItemsPage,
  MonthlyPlanningHomePage,
  PlanningOverviewPage,
} from "../../domains/monthly-planning/PlanningPages";
import { PlanningLayout } from "../../domains/monthly-planning/PlanningLayout";
import { BancoProvinciaLoanImportPage } from "../../domains/monthly-planning/banco-provincia/BancoProvinciaLoanImportPage";
import { QuickPlanTextImportPage } from "../../domains/monthly-planning/quick-text/QuickPlanTextImportPage";
import { ProfilesPage } from "../../domains/profiles/ProfilesPage";
import { TransactionRecategorizationPage } from "../../domains/recategorization/TransactionRecategorizationPage";
import { TransactionImportPage } from "../../domains/transactions/TransactionImportPage";
import { TransactionsPage } from "../../domains/transactions/TransactionsPage";
import { planningPathSegments, routePaths } from "./routePaths";

export type AppRoute = {
  path: string;
  element: JSX.Element;
  protected?: boolean;
  devOnly?: boolean;
  children?: AppRoute[];
};

export const publicRoutes: AppRoute[] = [
  {
    path: routePaths.devUser,
    element: <DevUserPage />,
  },
  {
    path: routePaths.profiles,
    element: <ProfilesPage />,
    devOnly: true,
  },
];

export const profileRoutes: AppRoute[] = [
  {
    path: "/profiles/:profileId/dashboard",
    element: <DashboardPage />,
    protected: true,
  },
  {
    path: "/profiles/:profileId/accounts",
    element: <AccountsPage />,
    protected: true,
  },
  {
    path: "/profiles/:profileId/categories",
    element: <CategoriesPage />,
    protected: true,
  },
  {
    path: "/profiles/:profileId/transactions",
    element: <TransactionsPage />,
    protected: true,
  },
  {
    path: "/profiles/:profileId/transactions/import",
    element: <TransactionImportPage />,
    protected: true,
  },
  {
    path: "/profiles/:profileId/transactions/recategorize",
    element: <TransactionRecategorizationPage />,
    protected: true,
  },
  {
    path: "/profiles/:profileId/budgets",
    element: <BudgetPage />,
    protected: true,
  },
  {
    path: "/profiles/:profileId/goals",
    element: <GoalsPage />,
    protected: true,
  },
  {
    path: "/profiles/:profileId/habits",
    element: <HabitsPage />,
    protected: true,
  },
  {
    path: "/profiles/:profileId/inflation",
    element: <InflationPage />,
    protected: true,
  },
  {
    path: "/profiles/:profileId/prestamos-externos",
    element: <ExternalLoansPage />,
    protected: true,
  },
  {
    path: "/profiles/:profileId/monthly-planning",
    element: <LegacyPlanningRedirect />,
    protected: true,
  },
];

export const planningRoute: AppRoute = {
  path: "/profiles/:profileId/planning",
  element: <PlanningLayout />,
  protected: true,
  children: [
    { path: "", element: <PlanningOverviewPage /> },
    {
      path: planningPathSegments.monthly,
      element: <MonthlyPlanningHomePage />,
    },
    {
      path: planningPathSegments.items,
      element: <MonthlyPlanItemsPage />,
    },
    {
      path: planningPathSegments.suggestions,
      element: <BudgetPlanningSuggestionsPage />,
    },
    {
      path: planningPathSegments.itemNew,
      element: <MonthlyPlanItemCreatePage />,
    },
    {
      path: planningPathSegments.itemEdit,
      element: <MonthlyPlanItemEditPage />,
    },
    {
      path: planningPathSegments.import,
      element: <MonthlyPlanImportPage />,
    },
    {
      path: planningPathSegments.quickText,
      element: <QuickPlanTextImportPage />,
    },
    {
      path: planningPathSegments.bancoProvincia,
      element: <BancoProvinciaLoanImportPage />,
    },
    {
      path: planningPathSegments.alerts,
      element: <MonthlyPlanAlertsPage />,
    },
    {
      path: planningPathSegments.convert,
      element: <MonthlyPlanConvertPage />,
    },
    {
      path: planningPathSegments.reconciliation,
      element: <MonthlyPlanReconciliationPage />,
    },
  ],
};

export const appRoutes = [...publicRoutes, planningRoute, ...profileRoutes];
