import { jsx as _jsx } from "react/jsx-runtime";
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
const qc = new QueryClient();
export const Providers = ({ children }) => _jsx(QueryClientProvider, { client: qc, children: children });
