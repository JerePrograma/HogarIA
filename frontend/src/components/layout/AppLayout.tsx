import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { NavLink, useNavigate, useParams } from 'react-router-dom';

type NavItem = {
  label: string;
  to: string;
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
        title: 'Principal',
        items: [
          { label: 'Panel mensual', to: `${base}/dashboard` },
          { label: 'Planificación', to: `${base}/planning` },
          { label: 'Movimientos', to: `${base}/transactions` },
        ],
      },
      {
        title: 'Organización',
        items: [
          { label: 'Presupuesto', to: `${base}/budgets` },
          { label: 'Cuentas', to: `${base}/accounts` },
          { label: 'Categorías', to: `${base}/categories` },
        ],
      },
      {
        title: 'Herramientas',
        items: [
          { label: 'Objetivos', to: `${base}/goals` },
          { label: 'Hábitos', to: `${base}/habits` },
          { label: 'Inflación', to: `${base}/inflation` },
          { label: 'Préstamos externos', to: `${base}/prestamos-externos` },
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
      <aside className="sidebar">
        <div className="panel-accent mb-4">
          <p className="label-ui">Sistema financiero personal</p>
          <h2>HogarIA</h2>
          <p className="texto-secundario">
            Organización, planificación y control operativo del hogar.
          </p>
        </div>

        <div className="surface-inset mb-4">
          <p className="label-ui">Modo desarrollo</p>

          <div className="mt-3 grid gap-2">
            <div>
              <p className="compact-muted">Usuario</p>
              <strong className="texto-principal">
                {devUserId?.slice(0, 8) ?? 'Sin usuario'}
              </strong>
            </div>

            <div>
              <p className="compact-muted">Perfil activo</p>
              <strong className="texto-principal">
                {selectedProfileId?.slice(0, 8) ?? 'Sin perfil'}
              </strong>
            </div>
          </div>
        </div>

        <button type="button" className="boton-secundario mb-3" onClick={toggleTheme}>
          {theme === 'dark' ? 'Usar modo claro' : 'Usar modo oscuro'}
        </button>

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
              </NavLink>
            ))}
          </nav>
        ))}

        <p className="nav-group">Cuenta</p>

        <NavLink
          to="/profiles"
          className={({ isActive }) =>
            `nav-item ${isActive ? 'nav-item-active' : ''}`.trim()
          }
        >
          <span className="nav-item-title">Perfiles</span>
        </NavLink>

        <button type="button" className="boton-fantasma mt-3 w-full" onClick={handleChangeUser}>
          Cambiar usuario
        </button>
      </aside>

      <main className="content">{children}</main>
    </div>
  );
}