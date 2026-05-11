import { NavLink, useNavigate, useParams } from 'react-router-dom';

export function AppLayout({ children }: { children: React.ReactNode }) {
  const { profileId } = useParams();
  const nav = useNavigate();
  const base = profileId ? `/profiles/${profileId}` : '';
  return <div className='app-shell'>
    <aside className='sidebar'>
      <h2>HogarIA</h2>
      <p>Modo desarrollo</p><p className='secondary-text'>Usuario: {localStorage.getItem('devUserId')?.slice(0, 8) ?? '-'}</p>
      <p>Perfil activo</p><p className='secondary-text'>{localStorage.getItem('selectedProfileId')?.slice(0, 8) ?? '-'}</p>
      {profileId && <>
        <p className='nav-group'>Principal</p>
        <NavLink to={`${base}/dashboard`}>Panel mensual</NavLink>
        <NavLink to={`${base}/planning`}>Planificación</NavLink>
        <NavLink to={`${base}/transactions`}>Movimientos</NavLink>
        <p className='nav-group'>Organización</p>
        <NavLink to={`${base}/budgets`}>Presupuesto</NavLink>
        <NavLink to={`${base}/accounts`}>Cuentas</NavLink>
        <NavLink to={`${base}/categories`}>Categorías</NavLink>
        <p className='nav-group'>Herramientas</p>
        <NavLink to={`${base}/imports/budget-excel`}>Carga guiada</NavLink>
        <NavLink to={`${base}/goals`}>Objetivos</NavLink>
        <NavLink to={`${base}/habits`}>Hábitos</NavLink>
        <NavLink to={`${base}/inflation`}>Inflación</NavLink>
        <NavLink to={`${base}/prestamos-externos`}>Préstamos externos</NavLink>
      </>}
      <NavLink to='/profiles'>Perfiles</NavLink>
      <button className='button-secondary' onClick={() => { localStorage.removeItem('devUserId'); localStorage.removeItem('selectedProfileId'); nav('/dev-user'); }}>Cambiar usuario</button>
    </aside>
    <main className='content'>{children}</main>
  </div>;
}
