import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { NavLink, useNavigate, useParams } from 'react-router-dom';

type NavItem = {
  label: string;
  to: string;
  description?: string;
};

type NavSection = {
  title: string;
  items: NavItem[];
};

type ThemeMode = 'light' | 'dark';

const getInitialTheme = (): ThemeMode => {
  const storedTheme = localStorage.getItem('hogariaTheme');

  if (storedTheme === 'light' || storedTheme === 'dark') {
    return storedTheme;
  }

  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
};

export function AppLayout({ children }: { children: ReactNode }) {
  const { profileId } = useParams();
  const navigate = useNavigate();
  const base = profileId ? `/profiles/${profileId}` : '';

  const [theme, setTheme] = useState<ThemeMode>(getInitialTheme);

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark');
    localStorage.setItem('hogariaTheme', theme);
  }, [theme]);

  const devUserId = localStorage.getItem('devUserId');
  const selectedProfileId = localStorage.getItem('selectedProfileId');

  const navSections = useMemo<NavSection[]>(() => {
    if (!profileId) {
      return [];
    }

    return [
      {
        title: 'Inicio',
        items: [{ label: 'Panel mensual', description: 'Resumen del período', to: `${base}/dashboard` }],
      },
      {
        title: 'Operación mensual',
        items: [
          { label: 'Planificación', description: 'Ingresos, gastos y pendientes', to: `${base}/planning` },
          { label: 'Movimientos', description: 'Registro real', to: `${base}/transactions` },
          { label: 'Presupuesto', description: 'Límites y estructura', to: `${base}/budgets` },
        ],
      },
      {
        title: 'Organización',
        items: [
          { label: 'Cuentas', to: `${base}/accounts` },
          { label: 'Categorías', to: `${base}/categories` },
        ],
      },
      {
        title: 'Seguimiento',
        items: [
          { label: 'Objetivos', to: `${base}/goals` },
          { label: 'Hábitos', to: `${base}/habits` },
          { label: 'Inflación', to: `${base}/inflation` },
          { label: 'Préstamos externos', description: 'Consulta integrada', to: `${base}/prestamos-externos` },
        ],
      },
    ];
  }, [base, profileId]);

  const handleChangeUser = () => {
    localStorage.removeItem('devUserId');
    localStorage.removeItem('selectedProfileId');
    navigate('/dev-user');
  };

  const toggleTheme = () => {
    setTheme((currentTheme) => (currentTheme === 'dark' ? 'light' : 'dark'));
  };

  return (
    <div className="app-shell">
      <aside className="sidebar" aria-label="Navegación principal">
        <div className="panel-accent mb-4">
          <p className="label-ui">HogarIA</p>
          <h2>Tu economía del hogar</h2>
        </div>

        <div className="surface-inset mb-4">
          <p className="label-ui">Sesión actual</p>

          <div className="mt-3 grid gap-2">
            <div>
              <p className="compact-muted">Usuario activo</p>
              <strong className="texto-principal">{devUserId ? 'Usuario seleccionado' : 'Sin usuario'}</strong>
              {devUserId && <p className="session-id">ID: {devUserId.slice(0, 8)}...</p>}
            </div>

            <div>
              <p className="compact-muted">Perfil activo</p>
              <strong className="texto-principal">{selectedProfileId ? 'Perfil seleccionado' : 'Sin perfil'}</strong>
              {selectedProfileId && <p className="session-id">ID: {selectedProfileId.slice(0, 8)}...</p>}
            </div>
          </div>
        </div>

        <div className="sidebar-nav" aria-label="Secciones de navegación">
          {navSections.map((section) => (
            <nav key={section.title} aria-label={section.title}>
              <p className="nav-group">{section.title}</p>

              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `nav-item ${isActive ? 'nav-item-active' : ''}`.trim()
                  }
                >
                  <span className="nav-item-title">{item.label}</span>
                  {item.description ? <span className="nav-item-description">{item.description}</span> : null}
                </NavLink>
              ))}
            </nav>
          ))}
        </div>

        <div className="sidebar-system" aria-label="Sistema">
          <p className="nav-group">Sistema</p>

          <NavLink
            to="/profiles"
            className={({ isActive }) =>
              `nav-item ${isActive ? 'nav-item-active' : ''}`.trim()
            }
          >
            <span className="nav-item-title">Perfiles</span>
          </NavLink>

          <button
            type="button"
            className="boton-secundario mt-3"
            onClick={toggleTheme}
            aria-label={theme === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
          >
            {theme === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
          </button>

          <button
            type="button"
            className="boton-fantasma mt-2 w-full"
            onClick={handleChangeUser}
            aria-label="Cambiar usuario activo"
          >
            Cambiar usuario
          </button>
        </div>
      </aside>

      <main className="content" aria-label="Contenido principal">{children}</main>
    </div>
  );
}
