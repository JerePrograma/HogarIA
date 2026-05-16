import { useQuery } from '@tanstack/react-query';
import { Navigate, Route, Routes, useParams } from 'react-router-dom';
import { listProfiles } from '../api/profilesApi';
import { AccountsPage } from '../features/accounts/AccountsPage';
import { BudgetPage } from '../features/budgets/BudgetPage';
import { CategoriesPage } from '../features/categories/CategoriesPage';
import { DashboardPage } from '../features/dashboard/DashboardPage';
import { DevUserPage } from '../features/dev-user/DevUserPage';
import { ExternalLoansPage } from '../features/external-loans/ExternalLoansPage';
import { GoalsPage } from '../features/goals/GoalsPage';
import { HabitsPage } from '../features/habits/HabitsPage';
import { InflationPage } from '../features/inflation/InflationPage';
import { MonthlyPlanningPage } from '../features/planning/MonthlyPlanningPage';
import { ProfilesPage } from '../features/profiles/ProfilesPage';
import { TransactionImportPage } from '../features/transactions/TransactionImportPage';
import { TransactionsPage } from '../features/transactions/TransactionsPage';

function DevGuard({ children }: { children: JSX.Element }) {
  return localStorage.getItem('devUserId') ? children : <Navigate to="/dev-user" replace />;
}

function ProfileGuard({ children }: { children: JSX.Element }) {
  const { profileId } = useParams();

  const profilesQuery = useQuery({
    queryKey: ['profiles'],
    queryFn: listProfiles,
  });

  if (profileId) {
    localStorage.setItem('selectedProfileId', profileId);
  }

  const selectedProfileId = profileId ?? localStorage.getItem('selectedProfileId');

  if (!selectedProfileId) {
    return <Navigate to="/profiles" replace />;
  }

  if (profilesQuery.isLoading) {
    return <p>Cargando perfiles...</p>;
  }

  if (profilesQuery.isSuccess) {
    const profileExists = profilesQuery.data.some((profile) => profile.id === selectedProfileId);
    if (!profileExists) {
      localStorage.removeItem('selectedProfileId');
      return <Navigate to="/profiles" replace />;
    }
  }

  return children;
}

function ProtectedRoute({ children }: { children: JSX.Element }) {
  return (
    <DevGuard>
      <ProfileGuard>{children}</ProfileGuard>
    </DevGuard>
  );
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
      <Route path="/profiles/:profileId/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
      <Route path="/profiles/:profileId/accounts" element={<ProtectedRoute><AccountsPage /></ProtectedRoute>} />
      <Route path="/profiles/:profileId/categories" element={<ProtectedRoute><CategoriesPage /></ProtectedRoute>} />
      <Route path="/profiles/:profileId/transactions" element={<ProtectedRoute><TransactionsPage /></ProtectedRoute>} />
      <Route
        path="/profiles/:profileId/transactions/import"
        element={<ProtectedRoute><TransactionImportPage /></ProtectedRoute>}
      />
      <Route path="/profiles/:profileId/budgets" element={<ProtectedRoute><BudgetPage /></ProtectedRoute>} />
      <Route path="/profiles/:profileId/goals" element={<ProtectedRoute><GoalsPage /></ProtectedRoute>} />
      <Route path="/profiles/:profileId/habits" element={<ProtectedRoute><HabitsPage /></ProtectedRoute>} />
      <Route path="/profiles/:profileId/inflation" element={<ProtectedRoute><InflationPage /></ProtectedRoute>} />
      <Route path="/profiles/:profileId/planning" element={<ProtectedRoute><MonthlyPlanningPage /></ProtectedRoute>} />
      <Route
        path="/profiles/:profileId/prestamos-externos"
        element={<ProtectedRoute><ExternalLoansPage /></ProtectedRoute>}
      />
      <Route path="*" element={<Navigate to="/profiles" replace />} />
    </Routes>
  );
}
