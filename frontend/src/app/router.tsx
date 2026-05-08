import { Navigate, Route, Routes, useParams } from 'react-router-dom';
import { DevUserPage } from '../features/dev-user/DevUserPage';
import { ProfilesPage } from '../features/profiles/ProfilesPage';
import { AccountsPage } from '../features/accounts/AccountsPage';
import { CategoriesPage } from '../features/categories/CategoriesPage';
import { TransactionsPage } from '../features/transactions/TransactionsPage';
import { DashboardPage } from '../features/dashboard/DashboardPage';
import { BudgetPage } from '../features/budgets/BudgetPage';

const DevGuard = ({ children }: { children: JSX.Element }) => localStorage.getItem('devUserId') ? children : <Navigate to='/dev-user' />;
const ProfileGuard = ({ children }: { children: JSX.Element }) => {
  const { profileId } = useParams();
  if (profileId) localStorage.setItem('selectedProfileId', profileId);
  if (!profileId && !localStorage.getItem('selectedProfileId')) return <Navigate to='/profiles' />;
  return children;
};

export const AppRouter = () => <Routes>
  <Route path='/dev-user' element={<DevUserPage />} />
  <Route path='/profiles' element={<DevGuard><ProfilesPage /></DevGuard>} />
  <Route path='/profiles/:profileId/dashboard' element={<DevGuard><ProfileGuard><DashboardPage /></ProfileGuard></DevGuard>} />
  <Route path='/profiles/:profileId/accounts' element={<DevGuard><ProfileGuard><AccountsPage /></ProfileGuard></DevGuard>} />
  <Route path='/profiles/:profileId/categories' element={<DevGuard><ProfileGuard><CategoriesPage /></ProfileGuard></DevGuard>} />
  <Route path='/profiles/:profileId/transactions' element={<DevGuard><ProfileGuard><TransactionsPage /></ProfileGuard></DevGuard>} />
  <Route path='/profiles/:profileId/budgets' element={<DevGuard><ProfileGuard><BudgetPage /></ProfileGuard></DevGuard>} />
  <Route path='*' element={<Navigate to='/profiles' />} />
</Routes>;
