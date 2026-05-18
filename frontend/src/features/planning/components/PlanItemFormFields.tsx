import {
  monthlyPlanPriorityOptions,
  monthlyPlanStatusOptions,
  monthlyPlanTypeOptions,
} from '../../../domain/financeOptions';
import type {
  Account,
  Category,
  MonthlyPlanItem,
  MonthlyPlanItemCreatePayload,
} from '../../../domain/types';

type Props = {
  form: MonthlyPlanItemCreatePayload;
  setForm: (form: MonthlyPlanItemCreatePayload) => void;
  accounts: Account[];
  categories: Category[];
  compact?: boolean;
};

const toNullableNumber = (value: string): number | null => {
  if (!value.trim()) {
    return null;
  }

  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
};

export function PlanItemFormFields({
  form,
  setForm,
  accounts,
  categories,
  compact = false,
}: Props) {
  const setNumber = (key: keyof MonthlyPlanItemCreatePayload, value: string) => {
    setForm({
      ...form,
      [key]: toNullableNumber(value),
    });
  };

  return (
    <div className="stack-ui">
      <section>
        {!compact && <h3>Qué es</h3>}

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
        {!compact && <h3>Cuándo y cuánto</h3>}

        <div className="form-grid">
          <label>
            Fecha esperada
            <input
              className="input-ui"
              type="date"
              value={form.expectedDate ?? ''}
              onChange={(event) =>
                setForm({ ...form, expectedDate: event.target.value || null })
              }
            />
          </label>

          <label>
            Monto exacto
            <input
              className="input-ui"
              type="number"
              value={form.amount ?? ''}
              onChange={(event) => setNumber('amount', event.target.value)}
              placeholder="Monto exacto"
            />
          </label>

          <label>
            Monto mínimo
            <input
              className="input-ui"
              type="number"
              value={form.minAmount ?? ''}
              onChange={(event) => setNumber('minAmount', event.target.value)}
              placeholder="Monto mínimo"
            />
          </label>

          <label>
            Monto máximo
            <input
              className="input-ui"
              type="number"
              value={form.maxAmount ?? ''}
              onChange={(event) => setNumber('maxAmount', event.target.value)}
              placeholder="Monto máximo"
            />
          </label>

          <label>
            Recupero monto
            <input
              className="input-ui"
              type="number"
              value={form.expectedRecoveryAmount ?? ''}
              onChange={(event) => setNumber('expectedRecoveryAmount', event.target.value)}
              placeholder="Recupero monto"
            />
          </label>

          <label>
            Recupero %
            <input
              className="input-ui"
              type="number"
              value={form.expectedRecoveryPercent ?? ''}
              onChange={(event) => setNumber('expectedRecoveryPercent', event.target.value)}
              placeholder="Recupero %"
            />
          </label>
        </div>
      </section>

      <section>
        {!compact && <h3>Detalle y clasificación</h3>}

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
              type="number"
              value={form.installmentNumber ?? ''}
              onChange={(event) => setNumber('installmentNumber', event.target.value)}
              placeholder="N° cuota"
            />
          </label>

          <label>
            Total cuotas
            <input
              className="input-ui"
              type="number"
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
  );
}