import { NavLink, useNavigate, useParams } from 'react-router-dom';

export function AppLayout({ children }: { children: React.ReactNode }) {
  const { profileId } = useParams();
  const nav = useNavigate();
  const base = profileId ? `/profiles/${profileId}` : '';
  return <div className='app-shell'>
    <aside className='sidebar'>
      <h2>HogarIA</h2>
      <p>Usuario: {localStorage.getItem('devUserId')?.slice(0, 8) ?? '-'}</p>
      <p>Perfil: {localStorage.getItem('selectedProfileId')?.slice(0, 8) ?? '-'}</p>
      {profileId && <>
        <NavLink to={`${base}/dashboard`}>Dashboard</NavLink>
        <NavLink to={`${base}/accounts`}>Cuentas</NavLink>
        <NavLink to={`${base}/categories`}>Categorías</NavLink>
        <NavLink to={`${base}/transactions`}>Movimientos</NavLink>
        <NavLink to={`${base}/budgets`}>Presupuesto</NavLink>
        <NavLink to={`${base}/goals`}>Objetivos</NavLink>
        <NavLink to={`${base}/habits`}>Hábitos</NavLink>
        <NavLink to={`${base}/inflation`}>Inflación</NavLink>
      </>}
      <NavLink to='/profiles'>Perfiles</NavLink>
      <button className='button-secondary' onClick={() => { localStorage.removeItem('devUserId'); localStorage.removeItem('selectedProfileId'); nav('/dev-user'); }}>Cambiar usuario</button>
    </aside>
    <main className='content'>{children}</main>
  </div>;
}
