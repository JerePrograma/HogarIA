import type { ReactNode } from 'react';
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

export function AppLayout({ children }: { children: ReactNode }) {
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

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">H</span>
          <div>
            <strong>HogarIA</strong>
            <small>Finanzas personales</small>
          </div>
        </div>

        <div className="sidebar-meta">
          <span>Usuario: {devUserId?.slice(0, 8) ?? '-'}</span>
          <span>Perfil: {selectedProfileId?.slice(0, 8) ?? '-'}</span>
        </div>

        <nav className="nav-menu" aria-label="Navegación principal">
          {profileId &&
            profileNavItems.map((item) => (
              <NavLink
                key={item.path}
                to={`${base}/${item.path}`}
                className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}
              >
                {item.label}
              </NavLink>
            ))}

          <NavLink to="/profiles" className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
            Perfiles
          </NavLink>
        </nav>

        <button type="button" className="button ghost sidebar-action" onClick={logout}>
          Cambiar usuario
        </button>
      </aside>

      <main className="content">{children}</main>
    </div>
  );
}