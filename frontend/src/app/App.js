import { jsx as _jsx } from "react/jsx-runtime";
import { BrowserRouter } from 'react-router-dom';
import { AppRouter } from './router';
import { Providers } from './providers';
export const App = () => _jsx(Providers, { children: _jsx(BrowserRouter, { children: _jsx(AppRouter, {}) }) });
