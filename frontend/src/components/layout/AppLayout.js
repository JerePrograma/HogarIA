import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { NavLink, useNavigate, useParams } from 'react-router-dom';
const profileNavItems = [
    { path: 'dashboard', label: 'Panel' },
    { path: 'transactions', label: 'Movimientos' },
    { path: 'budgets', label: 'Presupuesto' },
    { path: 'accounts', label: 'Cuentas' },
    { path: 'categories', label: 'Categorías' },
    { path: 'goals', label: 'Objetivos' },
    { path: 'habits', label: 'Hábitos' },
    { path: 'inflation', label: 'Inflación' },
];
export function AppLayout({ children }) {
    const { profileId } = useParams();
    const nav = useNavigate();
    const base = profileId ? `/profiles/${profileId}` : '';
    const devUserId = localStorage.getItem('devUserId');
    const selectedProfileId = localStorage.getItem('selectedProfileId');
    const logout = () => {
        localStorage.removeItem('devUserId');
        localStorage.removeItem('selectedProfileId');
        nav('/dev-user');
    };
    return (_jsxs("div", { className: "app-shell", children: [_jsxs("aside", { className: "sidebar", children: [_jsxs("div", { className: "brand", children: [_jsx("span", { className: "brand-mark", children: "H" }), _jsxs("div", { children: [_jsx("strong", { children: "HogarIA" }), _jsx("small", { children: "Finanzas personales" })] })] }), _jsxs("div", { className: "sidebar-meta", children: [_jsxs("span", { children: ["Usuario: ", devUserId?.slice(0, 8) ?? '-'] }), _jsxs("span", { children: ["Perfil: ", selectedProfileId?.slice(0, 8) ?? '-'] })] }), _jsxs("nav", { className: "nav-menu", "aria-label": "Navegaci\u00F3n principal", children: [profileId &&
                                profileNavItems.map((item) => (_jsx(NavLink, { to: `${base}/${item.path}`, className: ({ isActive }) => `nav-link${isActive ? ' active' : ''}`, children: item.label }, item.path))), _jsx(NavLink, { to: "/profiles", className: ({ isActive }) => `nav-link${isActive ? ' active' : ''}`, children: "Perfiles" })] }), _jsx("button", { type: "button", className: "button ghost sidebar-action", onClick: logout, children: "Cambiar usuario" })] }), _jsx("main", { className: "content", children: children })] }));
}
