import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { NavLink, useNavigate, useParams } from 'react-router-dom';
import { routePaths } from '../router/routePaths';

type NavItem = {
  label: string;
  to: string;
  description?: string;
  shortLabel?: string;
};

type NavSection = {
  title: string;
  summary: string;
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

export function AppShell({ children }: { children: ReactNode }) {
  const { profileId } = useParams();
  const navigate = useNavigate();

  const [theme, setTheme] = useState<ThemeMode>(getInitialTheme);
  const [mobileNavOpen, setMobileNavOpen] = useState(false);

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
        summary: 'Lectura y control',
        items: [
          {
            label: 'Inicio',
            description: 'Lectura ejecutiva del mes',
            shortLabel: 'Inicio',
            to: routePaths.dashboard(profileId),
          },
          {
            label: 'Centro de control',
            description: 'Calidad y revisión',
            shortLabel: 'Control',
            to: routePaths.controlCenter(profileId),
          },
        ],
      },
      {
        title: 'Movimientos',
        summary: 'Registro y depuración',
        items: [
          {
            label: 'Movimientos',
            description: 'Registro real',
            shortLabel: 'Listado',
            to: routePaths.transactions(profileId),
          },
          {
            label: 'Importar',
            description: 'Carga guiada',
            shortLabel: 'Importar',
            to: routePaths.transactionImport(profileId),
          },
          {
            label: 'Recategorizar',
            description: 'Corrección masiva',
            shortLabel: 'Recat.',
            to: routePaths.transactionRecategorize(profileId),
          },
        ],
      },
      {
        title: 'Planificación',
        summary: 'Mes, presupuesto y desvíos',
        items: [
          {
            label: 'Planificación',
            description: 'Decidir y reconciliar',
            shortLabel: 'Plan',
            to: routePaths.planning(profileId),
          },
          {
            label: 'Presupuesto',
            description: 'Límites y control',
            shortLabel: 'Presup.',
            to: routePaths.budgets(profileId),
          },
        ],
      },
      {
        title: 'Organización',
        summary: 'Catálogo financiero',
        items: [
          { label: 'Cuentas', description: 'Origen del dinero', shortLabel: 'Cuentas', to: routePaths.accounts(profileId) },
          { label: 'Categorías', description: 'Reglas y jerarquía', shortLabel: 'Categorías', to: routePaths.categories(profileId) },
        ],
      },
      {
        title: 'Seguimiento',
        summary: 'Evolución y contexto',
        items: [
          { label: 'Objetivos', description: 'Metas de ahorro', shortLabel: 'Objetivos', to: routePaths.goals(profileId) },
          { label: 'Hábitos', description: 'Rutinas financieras', shortLabel: 'Hábitos', to: routePaths.habits(profileId) },
          { label: 'Inflación', description: 'Contexto de precios', shortLabel: 'Inflación', to: routePaths.inflation(profileId) },
          {
            label: 'Préstamos externos',
            description: 'Consulta integrada',
            shortLabel: 'Préstamos',
            to: routePaths.externalLoans(profileId),
          },
        ],
      },
    ];
  }, [profileId]);

  const handleChangeUser = () => {
    localStorage.removeItem('devUserId');
    localStorage.removeItem('selectedProfileId');
    navigate(routePaths.devUser);
  };

  const toggleTheme = () => {
    setTheme((currentTheme) => (currentTheme === 'dark' ? 'light' : 'dark'));
  };

  return (
    <div className="app-shell">
      <header className="mobile-shell-header">
        <div>
          <p className="label-ui">HogarIA</p>
          <strong>Economía del hogar</strong>
        </div>

        <button
          type="button"
          className="boton-secundario mobile-nav-toggle"
          aria-expanded={mobileNavOpen}
          aria-controls="app-navigation"
          onClick={() => setMobileNavOpen((open) => !open)}
        >
          {mobileNavOpen ? 'Cerrar' : 'Menú'}
        </button>
      </header>

      <aside
        id="app-navigation"
        className={`sidebar ${mobileNavOpen ? 'sidebar-open' : ''}`}
        aria-label="Navegación principal"
      >
        <div className="sidebar-brand">
          <div>
            <p className="label-ui">HogarIA</p>
            <h2>Economía del hogar</h2>
          </div>

          <button
            type="button"
            className="sidebar-icon-button"
            onClick={toggleTheme}
            aria-label={theme === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
          >
            {theme === 'dark' ? 'Claro' : 'Oscuro'}
          </button>
        </div>

        <div className="sidebar-session">
          <div>
            <p className="label-ui">Sesión</p>
            <strong className="texto-principal">{devUserId ? 'Usuario activo' : 'Sin usuario'}</strong>
            {devUserId && <p className="session-id">{devUserId.slice(0, 8)}...</p>}
          </div>

          <div>
            <p className="label-ui">Perfil</p>
            <strong className="texto-principal">{selectedProfileId ? 'Seleccionado' : 'Sin perfil'}</strong>
            {selectedProfileId && <p className="session-id">{selectedProfileId.slice(0, 8)}...</p>}
          </div>
        </div>

        <div className="sidebar-nav" aria-label="Secciones de navegación">
          {navSections.map((section) => (
            <nav key={section.title} className="nav-section" aria-label={section.title}>
              <div className="nav-section-heading">
                <p className="nav-group">{section.title}</p>
                <span>{section.summary}</span>
              </div>

              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  onClick={() => setMobileNavOpen(false)}
                  className={({ isActive }) =>
                    `nav-item ${isActive ? 'nav-item-active' : ''}`.trim()
                  }
                >
                  <span className="nav-item-title" data-short-label={item.shortLabel ?? item.label}>{item.label}</span>
                  {item.description ? <span className="nav-item-description">{item.description}</span> : null}
                </NavLink>
              ))}
            </nav>
          ))}
        </div>

        <div className="sidebar-system nav-section" aria-label="Sistema">
          <div className="nav-section-heading">
            <p className="nav-group">Sistema</p>
            <span>Perfil y usuario</span>
          </div>

          <NavLink
            to={routePaths.profiles}
            onClick={() => setMobileNavOpen(false)}
            className={({ isActive }) =>
              `nav-item ${isActive ? 'nav-item-active' : ''}`.trim()
            }
          >
            <span className="nav-item-title">Perfiles</span>
          </NavLink>

          <button
            type="button"
            className="boton-secundario sidebar-theme-button"
            onClick={toggleTheme}
            aria-label={theme === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
          >
            {theme === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
          </button>

          <button
            type="button"
            className="boton-fantasma"
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

export const AppLayout = AppShell;
