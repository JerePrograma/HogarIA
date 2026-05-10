import { useState } from 'react';
import { monthlyPlanPriorityOptions, monthlyPlanStatusOptions, monthlyPlanTypeOptions } from '../../../domain/financeOptions';
import type { Account, Category, MonthlyPlanItem, MonthlyPlanItemCreatePayload, PlanningSuggestionResponse, QuickCapturePreviewResponse } from '../../../domain/types';
import { confidenceMeta, formatPlanAmount, formatPlanRecovery } from '../planningUtils';
import { getMonthlyPlanSuggestions } from '../../../api/monthlyPlanSuggestionsApi';

type Props = { profileId: string; preview: QuickCapturePreviewResponse; form: MonthlyPlanItemCreatePayload; setForm: (next: MonthlyPlanItemCreatePayload) => void; accounts: Account[]; categories: Category[]; onConfirm: () => void };

export function QuickCapturePreviewForm({ profileId, preview, form, setForm, accounts, categories, onConfirm }: Props) {
  const confidence = confidenceMeta(preview.confidence);
  const [sugg, setSugg] = useState<PlanningSuggestionResponse | null>(null);
  const load = async () => setSugg(await getMonthlyPlanSuggestions(profileId, { type: form.type, title: form.title, counterparty: form.counterparty ?? null, amount: form.amount ?? null, minAmount: form.minAmount ?? null, maxAmount: form.maxAmount ?? null, expectedRecoveryAmount: form.expectedRecoveryAmount ?? null, expectedRecoveryPercent: form.expectedRecoveryPercent ?? null }));
  const apply = () => { if (!sugg) return; setForm({ ...form, accountId: sugg.accountSuggestion?.id ?? form.accountId ?? null, categoryId: sugg.categorySuggestion?.id ?? form.categoryId ?? null }); };
  const setNumber = (key: keyof MonthlyPlanItemCreatePayload, value: string) => setForm({ ...form, [key]: value.trim() ? Number(value) : null });

  return <div className='page-stack'>
    <p><b>Confianza:</b> <span className={`badge ${confidence.className}`}>{confidence.label}</span></p>
    {preview.confidence === 'LOW' ? <p className='warning-box'>Revisá bien antes de guardar: la interpretación tiene baja confianza.</p> : null}
    {preview.warnings.length > 0 && <ul>{preview.warnings.map((w, i) => <li key={i}>{w}</li>)}</ul>}

    <h4>Qué es</h4>
    <div className='form-grid'>
      <select className='select' value={form.type} onChange={e => setForm({ ...form, type: e.target.value as MonthlyPlanItem['type'] })}>{monthlyPlanTypeOptions.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}</select>
      <input className='input' value={form.title} onChange={e => setForm({ ...form, title: e.target.value })} placeholder='Título' />
      <select className='select' value={form.priority} onChange={e => setForm({ ...form, priority: e.target.value as MonthlyPlanItem['priority'] })}>{monthlyPlanPriorityOptions.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}</select>
      <select className='select' value={form.status} onChange={e => setForm({ ...form, status: e.target.value as MonthlyPlanItem['status'] })}>{monthlyPlanStatusOptions.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}</select>
    </div>
    <h4>Cuándo y cuánto</h4><div className='form-grid'><input className='input' type='date' value={form.expectedDate ?? ''} onChange={e => setForm({ ...form, expectedDate: e.target.value || null })} /><input className='input' placeholder='Monto exacto' value={form.amount ?? ''} onChange={(e) => setNumber('amount', e.target.value)} /><input className='input' placeholder='Monto mínimo' value={form.minAmount ?? ''} onChange={(e) => setNumber('minAmount', e.target.value)} /><input className='input' placeholder='Monto máximo' value={form.maxAmount ?? ''} onChange={(e) => setNumber('maxAmount', e.target.value)} /><input className='input' placeholder='Recupero monto' value={form.expectedRecoveryAmount ?? ''} onChange={(e) => setNumber('expectedRecoveryAmount', e.target.value)} /><input className='input' placeholder='Recupero %' value={form.expectedRecoveryPercent ?? ''} onChange={(e) => setNumber('expectedRecoveryPercent', e.target.value)} /></div>
    <h4>Detalle</h4><div className='form-grid'><input className='input' placeholder='Contraparte' value={form.counterparty ?? ''} onChange={(e) => setForm({ ...form, counterparty: e.target.value || null })} /><input className='input' placeholder='N° cuota' value={form.installmentNumber ?? ''} onChange={(e) => setNumber('installmentNumber', e.target.value)} /><input className='input' placeholder='Total cuotas' value={form.installmentTotal ?? ''} onChange={(e) => setNumber('installmentTotal', e.target.value)} /></div>
    <h4>Clasificación</h4><div className='form-grid'><select className='select' value={form.accountId ?? ''} onChange={e => setForm({ ...form, accountId: e.target.value || null })}><option value=''>Cuenta opcional</option>{accounts.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}</select><select className='select' value={form.categoryId ?? ''} onChange={e => setForm({ ...form, categoryId: e.target.value || null })}><option value=''>Categoría opcional</option>{categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</select></div>
    <div className='action-row'><button className='button-secondary' onClick={() => void load()}>Pedir sugerencias</button>{sugg && <button className='button-primary' onClick={apply}>Aplicar sugerencias</button>}</div>
    {!form.accountId || !form.categoryId ? <p className='secondary-text'>Podés guardarlo igual como planificación. Para convertirlo después vas a necesitar cuenta y categoría.</p> : null}
    {sugg ? <div><p><b>Motivos de sugerencia</b></p><ul>{sugg.reasons.map((reason, index) => <li key={index}>{reason}</li>)}</ul></div> : null}
    <p>Monto detectado: {formatPlanAmount(form as MonthlyPlanItem)}</p><p>Recupero detectado: {formatPlanRecovery(form as MonthlyPlanItem)}</p><button className='button-primary' onClick={onConfirm} disabled={!form.title.trim()}>Guardar en planificación</button>
  </div>;
}
