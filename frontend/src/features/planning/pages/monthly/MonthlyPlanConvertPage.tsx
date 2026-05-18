import { Link } from 'react-router-dom';
import { useMonthlyPlanItemActions } from '../../../../hooks/useMonthlyPlanItemActions';
import { useStructuredPlanItemDraft } from '../../../../hooks/useStructuredPlanItemDraft';
import { usePlanningData } from '../../hooks/usePlanningData';
import { canConvertPlanItem, getPlanItemMissingLabels } from '../../planningUtils';
import { preservePlanningPeriodParams } from '../../planningSearchParams';
import { planningRoutes } from '../../planningRoutes';
import { buildPlanningPath } from '../../utils/buildPlanningPath';

export function MonthlyPlanConvertPage(){ const {items,profileId,year,month,invalidatePlanningViews,searchParams }=usePlanningData(); const draft=useStructuredPlanItemDraft(year,month); const actions=useMonthlyPlanItemActions({profileId,year,month,form:draft.form,resetFormAfterCreate:draft.resetAfterCreate,invalidatePlanningViews}); const convertible = items.filter(canConvertPlanItem); const blocked = items.filter((item) => !canConvertPlanItem(item)); const editLink = (id: string, mode: 'classification'|'amount') => buildPlanningPath(planningRoutes.itemEdit(profileId,id), new URLSearchParams([...preservePlanningPeriodParams(searchParams).entries(), ['mode', mode]])); return <section className='panel'><h2>Revisión de conversión</h2>{actions.actionErrorMessage ? <p role='alert'>{actions.actionErrorMessage}</p> : null}<ul>{convertible.map((item)=><li key={item.id}><strong>{item.title}</strong><button type='button' className='boton-principal' disabled={actions.pendingActionId===item.id} onClick={()=>{ if (!window.confirm('¿Querés convertir este ítem ahora?')) return; actions.convert(item.id); }}>Convertir</button></li>)}</ul><h3>Bloqueados</h3><ul>{blocked.slice(0,20).map((item)=><li key={item.id}>{item.title} — {getPlanItemMissingLabels(item).join(', ')} <Link to={editLink(item.id, !item.amount && !item.minAmount ? 'amount' : 'classification')}>Corregir datos</Link></li>)}</ul></section>; }
