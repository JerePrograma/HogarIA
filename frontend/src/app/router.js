import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Navigate, Route, Routes, useParams } from 'react-router-dom';
import { DevUserPage } from '../features/dev-user/DevUserPage';
import { ProfilesPage } from '../features/profiles/ProfilesPage';
import { AccountsPage } from '../features/accounts/AccountsPage';
import { CategoriesPage } from '../features/categories/CategoriesPage';
import { TransactionsPage } from '../features/transactions/TransactionsPage';
import { DashboardPage } from '../features/dashboard/DashboardPage';
import { BudgetPage } from '../features/budgets/BudgetPage';
import { GoalsPage } from '../features/goals/GoalsPage';
import { HabitsPage } from '../features/habits/HabitsPage';
import { InflationPage } from '../features/inflation/InflationPage';
const DevGuard = ({ children }) => localStorage.getItem('devUserId') ? children : _jsx(Navigate, { to: '/dev-user' });
const ProfileGuard = ({ children }) => {
    const { profileId } = useParams();
    if (profileId)
        localStorage.setItem('selectedProfileId', profileId);
    if (!profileId && !localStorage.getItem('selectedProfileId'))
        return _jsx(Navigate, { to: '/profiles' });
    return children;
};
export const AppRouter = () => _jsxs(Routes, { children: [_jsx(Route, { path: '/dev-user', element: _jsx(DevUserPage, {}) }), _jsx(Route, { path: '/profiles', element: _jsx(DevGuard, { children: _jsx(ProfilesPage, {}) }) }), _jsx(Route, { path: '/profiles/:profileId/dashboard', element: _jsx(DevGuard, { children: _jsx(ProfileGuard, { children: _jsx(DashboardPage, {}) }) }) }), _jsx(Route, { path: '/profiles/:profileId/accounts', element: _jsx(DevGuard, { children: _jsx(ProfileGuard, { children: _jsx(AccountsPage, {}) }) }) }), _jsx(Route, { path: '/profiles/:profileId/categories', element: _jsx(DevGuard, { children: _jsx(ProfileGuard, { children: _jsx(CategoriesPage, {}) }) }) }), _jsx(Route, { path: '/profiles/:profileId/transactions', element: _jsx(DevGuard, { children: _jsx(ProfileGuard, { children: _jsx(TransactionsPage, {}) }) }) }), _jsx(Route, { path: '/profiles/:profileId/budgets', element: _jsx(DevGuard, { children: _jsx(ProfileGuard, { children: _jsx(BudgetPage, {}) }) }) }), _jsx(Route, { path: '/profiles/:profileId/goals', element: _jsx(DevGuard, { children: _jsx(ProfileGuard, { children: _jsx(GoalsPage, {}) }) }) }), _jsx(Route, { path: '/profiles/:profileId/habits', element: _jsx(DevGuard, { children: _jsx(ProfileGuard, { children: _jsx(HabitsPage, {}) }) }) }), _jsx(Route, { path: '/profiles/:profileId/inflation', element: _jsx(DevGuard, { children: _jsx(ProfileGuard, { children: _jsx(InflationPage, {}) }) }) }), _jsx(Route, { path: '*', element: _jsx(Navigate, { to: '/profiles' }) })] });
