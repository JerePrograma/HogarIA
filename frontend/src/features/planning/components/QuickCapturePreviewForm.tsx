import { useState } from 'react';
import { getMonthlyPlanSuggestions } from '../../../api/monthlyPlanSuggestionsApi';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import {
  monthlyPlanPriorityOptions,
  monthlyPlanStatusOptions,
  monthlyPlanTypeOptions,
} from '../../../domain/financeOptions';
import { monthLabels } from '../../../domain/financeLabels';
import type {
  Account,
  Category,
  MonthlyPlanItem,
  MonthlyPlanItemCreatePayload,
  PlanningSuggestionResponse,
  QuickCapturePreviewResponse,
} from '../../../domain/types';
import { confidenceMeta, formatPlanAmount, formatPlanRecovery } from '../planningUtils';

type Props = {
  profileId: string;
  preview: QuickCapturePreviewResponse;
  form: MonthlyPlanItemCreatePayload;
  setForm: (next: MonthlyPlanItemCreatePayload) => void;
  accounts: Account[];
  categories: Category[];
  onConfirm: () => void;
  isConfirming?: boolean;
  error?: string | null;
};

export function QuickCapturePreviewForm({
  profileId,
  preview,
  form,
  setForm,
  accounts,
  categories,
  onConfirm,
  isConfirming = false,
  error,
}: Props) {
  const confidence = confidenceMeta(preview.confidence);
  const [suggestion, setSuggestion] = useState<PlanningSuggestionResponse | null>(null);
  const [suggestionError, setSuggestionError] = useState('');

  const setNumber = (key: keyof MonthlyPlanItemCreatePayload, value: string) => {
    setForm({ ...form, [key]: value.trim() ? Number(value) : null });
  };

  const setExpectedDate = (value: string) => {
    const period = periodFromDate(value);

    setForm({
      ...form,
      expectedDate: value || null,
      ...(period
        ? {
            periodYear: period.year,
            periodMonth: period.month,
          }
        : {}),
    });
  };

  const loadSuggestions = async () => {
    try {
      setSuggestionError('');
      const response = await getMonthlyPlanSuggestions(profileId, {
        type: form.type,
        title: form.title,
        counterparty: form.counterparty ?? null,
        amount: form.amount ?? null,
        minAmount: form.minAmount ?? null,
        maxAmount: form.maxAmount ?? null,
        expectedRecoveryAmount: form.expectedRecoveryAmount ?? null,
        expectedRecoveryPercent: form.expectedRecoveryPercent ?? null,
      });

      setSuggestion(response);
    } catch {
      setSuggestion(null);
      setSuggestionError('No se pudieron obtener sugerencias en este momento.');
    }
  };

  const applySuggestions = () => {
    if (!suggestion) return;

    setForm({
      ...form,
      accountId: suggestion.accountSuggestion?.id ?? form.accountId ?? null,
      categoryId: suggestion.categorySuggestion?.id ?? form.categoryId ?? null,
    });
  };

  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Vista previa</p>
          <h2>Revisión antes de guardar</h2>
        </div>

        <StatusBadge tone={confidence.tone} label={`Confianza ${confidence.label}`} />
      </div>

      {preview.confidence === 'LOW' ? (
        <p className="mensaje-warning">
          Revisá bien antes de guardar: la interpretación tiene baja confianza.
        </p>
      ) : null}

      {preview.warnings.length > 0 ? (
        <div className="surface-inset">
          <p className="label-ui">Advertencias</p>
          <ul className="mb-0 mt-2">
            {preview.warnings.map((warning, index) => (
              <li key={index}>{warning}</li>
            ))}
          </ul>
        </div>
      ) : null}

      <div className="stack-ui">
        <section>
          <h3>Qué es</h3>

          <div className="form-grid">
            <label>
              Tipo
              <select
                className="input-ui"
                value={form.type}
                onChange={(event) =>
                  setForm({ ...form, type: event.target.value as MonthlyPlanItem['type'] })
                }
              >
                {monthlyPlanTypeOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Título
              <input
                className="input-ui"
                value={form.title}
                onChange={(event) => setForm({ ...form, title: event.target.value })}
                placeholder="Título"
              />
            </label>

            <label>
              Prioridad
              <select
                className="input-ui"
                value={form.priority}
                onChange={(event) =>
                  setForm({
                    ...form,
                    priority: event.target.value as MonthlyPlanItem['priority'],
                  })
                }
              >
                {monthlyPlanPriorityOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Estado
              <select
                className="input-ui"
                value={form.status}
                onChange={(event) =>
                  setForm({ ...form, status: event.target.value as MonthlyPlanItem['status'] })
                }
              >
                {monthlyPlanStatusOptions.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </section>

        <section>
          <h3>Cuándo y cuánto</h3>

          <div className="form-grid">
            <label>
              Fecha esperada
              <input
                className="input-ui"
                type="date"
                value={form.expectedDate ?? ''}
                onChange={(event) => setExpectedDate(event.target.value)}
              />
            </label>

            <label>
              Período operativo
              <div className="form-row">
                <input
                  className="input-ui"
                  type="number"
                  min="2000"
                  max="2100"
                  value={form.periodYear}
                  onChange={(event) =>
                    setForm({ ...form, periodYear: Number(event.target.value) })
                  }
                  aria-label="Año operativo"
                />
                <select
                  className="input-ui"
                  value={form.periodMonth}
                  onChange={(event) =>
                    setForm({ ...form, periodMonth: Number(event.target.value) })
                  }
                  aria-label="Mes operativo"
                >
                  {Object.entries(monthLabels).map(([month, label]) => (
                    <option key={month} value={month}>
                      {label}
                    </option>
                  ))}
                </select>
              </div>
            </label>

            <label>
              Monto exacto
              <input
                className="input-ui"
                value={form.amount ?? ''}
                onChange={(event) => setNumber('amount', event.target.value)}
                placeholder="Monto exacto"
              />
            </label>

            <label>
              Monto mínimo
              <input
                className="input-ui"
                value={form.minAmount ?? ''}
                onChange={(event) => setNumber('minAmount', event.target.value)}
                placeholder="Monto mínimo"
              />
            </label>

            <label>
              Monto máximo
              <input
                className="input-ui"
                value={form.maxAmount ?? ''}
                onChange={(event) => setNumber('maxAmount', event.target.value)}
                placeholder="Monto máximo"
              />
            </label>

            <label>
              Recupero monto
              <input
                className="input-ui"
                value={form.expectedRecoveryAmount ?? ''}
                onChange={(event) => setNumber('expectedRecoveryAmount', event.target.value)}
                placeholder="Recupero monto"
              />
            </label>

            <label>
              Recupero %
              <input
                className="input-ui"
                value={form.expectedRecoveryPercent ?? ''}
                onChange={(event) => setNumber('expectedRecoveryPercent', event.target.value)}
                placeholder="Recupero %"
              />
            </label>
          </div>
        </section>

        <section>
          <h3>Detalle y clasificación</h3>

          <div className="form-grid">
            <label>
              Contraparte
              <input
                className="input-ui"
                value={form.counterparty ?? ''}
                onChange={(event) =>
                  setForm({ ...form, counterparty: event.target.value || null })
                }
                placeholder="Contraparte"
              />
            </label>

            <label>
              N° cuota
              <input
                className="input-ui"
                value={form.installmentNumber ?? ''}
                onChange={(event) => setNumber('installmentNumber', event.target.value)}
                placeholder="N° cuota"
              />
            </label>

            <label>
              Total cuotas
              <input
                className="input-ui"
                value={form.installmentTotal ?? ''}
                onChange={(event) => setNumber('installmentTotal', event.target.value)}
                placeholder="Total cuotas"
              />
            </label>

            <label>
              Cuenta
              <select
                className="input-ui"
                value={form.accountId ?? ''}
                onChange={(event) => setForm({ ...form, accountId: event.target.value || null })}
              >
                <option value="">Cuenta opcional</option>
                {accounts.map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.name}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Categoría
              <select
                className="input-ui"
                value={form.categoryId ?? ''}
                onChange={(event) => setForm({ ...form, categoryId: event.target.value || null })}
              >
                <option value="">Categoría opcional</option>
                {categories.map((category) => (
                  <option key={category.id} value={category.id}>
                    {category.name}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </section>
      </div>

      <div className="surface-inset">
        <p>
          <strong>Monto detectado:</strong> {formatPlanAmount(form as MonthlyPlanItem)}
        </p>
        <p className="mb-0">
          <strong>Recupero detectado:</strong> {formatPlanRecovery(form as MonthlyPlanItem)}
        </p>
      </div>

      <div className="action-row">
        <button type="button" className="boton-secundario" onClick={() => void loadSuggestions()}>
          Pedir sugerencias
        </button>

        {suggestion ? (
          <button type="button" className="boton-principal" onClick={applySuggestions}>
            Aplicar sugerencias
          </button>
        ) : null}
      </div>

      {suggestionError ? <p className="mensaje-error">{suggestionError}</p> : null}

      {!form.accountId || !form.categoryId ? (
        <p className="mensaje-info">
          Podés guardarlo igual como planificación. Para convertirlo después vas a necesitar cuenta
          y categoría.
        </p>
      ) : null}

      {suggestion ? (
        <div className="surface-inset">
          <p className="label-ui">Motivos de sugerencia</p>
          <ul className="mb-0 mt-2">
            {suggestion.reasons.map((reason, index) => (
              <li key={index}>{reason}</li>
            ))}
          </ul>
        </div>
      ) : null}

      {error ? <p className="mensaje-error">{error}</p> : null}

      <button
        type="button"
        className="boton-principal"
        onClick={onConfirm}
        disabled={!form.title.trim() || isConfirming}
      >
        {isConfirming ? 'Guardando...' : 'Guardar en planificación'}
      </button>
    </section>
  );
}

function periodFromDate(value: string): { year: number; month: number } | null {
  const match = /^(\d{4})-(\d{2})-\d{2}$/.exec(value);

  if (!match) return null;

  return {
    year: Number(match[1]),
    month: Number(match[2]),
  };
}
