import { useQuery } from "@tanstack/react-query";
import { Navigate, Route, Routes, useParams } from "react-router-dom";
import { listProfiles } from "../api/profilesApi";
import { AccountsPage } from "../features/accounts/AccountsPage";
import { BudgetPage } from "../features/budgets/BudgetPage";
import { CategoriesPage } from "../features/categories/CategoriesPage";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import { DevUserPage } from "../features/dev-user/DevUserPage";
import { ExternalLoansPage } from "../features/external-loans/ExternalLoansPage";
import { GoalsPage } from "../features/goals/GoalsPage";
import { HabitsPage } from "../features/habits/HabitsPage";
import { InflationPage } from "../features/inflation/InflationPage";
import { PlanningLayout } from "../features/planning/PlanningLayout";
import {
  LegacyPlanningRedirect,
  MonthlyPlanAlertsPage,
  MonthlyPlanConvertPage,
  MonthlyPlanImportPage,
  MonthlyPlanItemCreatePage,
  MonthlyPlanItemEditPage,
  MonthlyPlanItemsPage,
  MonthlyPlanningHomePage,
  PlanningOverviewPage,
} from "../features/planning/PlanningPages";
import { MonthlyPlanReconciliationPage } from "../features/planning/MonthlyPlanReconciliationPage";
import { ProfilesPage } from "../features/profiles/ProfilesPage";
import { TransactionImportPage } from "../features/transactions/TransactionImportPage";
import { TransactionRecategorizationPage } from "../features/recategorization/TransactionRecategorizationPage";
import { TransactionsPage } from "../features/transactions/TransactionsPage";

import { useEffect } from "react";
import { queryKeys } from "../domain/queryKeys";

type GuardProps = {
  children: JSX.Element;
};

function DevGuard({ children }: GuardProps) {
  return localStorage.getItem("devUserId") ? (
    children
  ) : (
    <Navigate to="/dev-user" replace />
  );
}

function ProfileGuard({ children }: GuardProps) {
  const { profileId } = useParams();

  const profilesQuery = useQuery({
    queryKey: queryKeys.profiles,
    queryFn: listProfiles,
  });

  useEffect(() => {
    if (profileId) {
      localStorage.setItem("selectedProfileId", profileId);
    }
  }, [profileId]);

  const selectedProfileId =
    profileId ?? localStorage.getItem("selectedProfileId");

  if (!selectedProfileId) {
    return <Navigate to="/profiles" replace />;
  }

  if (profilesQuery.isLoading) {
    return <p className="muted">Cargando perfiles...</p>;
  }

  if (profilesQuery.isSuccess) {
    const profileExists = profilesQuery.data.some(
      (profile) => profile.id === selectedProfileId,
    );

    if (!profileExists) {
      localStorage.removeItem("selectedProfileId");
      return <Navigate to="/profiles" replace />;
    }
  }

  return children;
}

function ProtectedRoute({ children }: GuardProps) {
  return (
    <DevGuard>
      <ProfileGuard>{children}</ProfileGuard>
    </DevGuard>
  );
}

function protectedElement(element: JSX.Element) {
  return <ProtectedRoute>{element}</ProtectedRoute>;
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/dev-user" element={<DevUserPage />} />
      <Route
        path="/profiles"
        element={
          <DevGuard>
            <ProfilesPage />
          </DevGuard>
        }
      />
      <Route
        path="/profiles/:profileId/dashboard"
        element={
          <ProtectedRoute>
            <DashboardPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profiles/:profileId/accounts"
        element={
          <ProtectedRoute>
            <AccountsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profiles/:profileId/categories"
        element={
          <ProtectedRoute>
            <CategoriesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profiles/:profileId/transactions"
        element={
          <ProtectedRoute>
            <TransactionsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profiles/:profileId/transactions/import"
        element={
          <ProtectedRoute>
            <TransactionImportPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profiles/:profileId/transactions/recategorize"
        element={
          <ProtectedRoute>
            <TransactionRecategorizationPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profiles/:profileId/budgets"
        element={
          <ProtectedRoute>
            <BudgetPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profiles/:profileId/goals"
        element={
          <ProtectedRoute>
            <GoalsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profiles/:profileId/habits"
        element={
          <ProtectedRoute>
            <HabitsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profiles/:profileId/inflation"
        element={
          <ProtectedRoute>
            <InflationPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profiles/:profileId/planning"
        element={
          <ProtectedRoute>
            <PlanningLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<PlanningOverviewPage />} />
        <Route path="monthly" element={<MonthlyPlanningHomePage />} />
        <Route path="monthly/items" element={<MonthlyPlanItemsPage />} />
        <Route
          path="monthly/items/new"
          element={<MonthlyPlanItemCreatePage />}
        />
        <Route
          path="monthly/items/:itemId/edit"
          element={<MonthlyPlanItemEditPage />}
        />
        <Route path="monthly/import" element={<MonthlyPlanImportPage />} />
        <Route path="monthly/alerts" element={<MonthlyPlanAlertsPage />} />
        <Route path="monthly/convert" element={<MonthlyPlanConvertPage />} />
        <Route
          path="monthly/reconciliation"
          element={<MonthlyPlanReconciliationPage />}
        />
      </Route>
      <Route
        path="/profiles/:profileId/monthly-planning"
        element={
          <ProtectedRoute>
            <LegacyPlanningRedirect />
          </ProtectedRoute>
        }
      />
      <Route
        path="/profiles/:profileId/prestamos-externos"
        element={
          <ProtectedRoute>
            <ExternalLoansPage />
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/profiles" replace />} />
    </Routes>
  );
}
