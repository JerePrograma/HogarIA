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
  onCreate: () => void;
  isCreating?: boolean;
  error?: string | null;
};

const toNullableNumber = (value: string): number | null => (value ? Number(value) : null);

export function StructuredPlanItemForm({
  form,
  setForm,
  accounts,
  categories,
  onCreate,
  isCreating = false,
  error,
}: Props) {
  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Carga manual</p>
          <h2>Alta estructurada</h2>
          <p className="secondary-text">
            Usá este formulario cuando quieras cargar un ítem con datos controlados desde el inicio.
          </p>
        </div>
      </div>

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
            placeholder="Título"
            value={form.title}
            onChange={(event) => setForm({ ...form, title: event.target.value })}
          />
        </label>

        <label>
          Fecha esperada
          <input
            className="input-ui"
            type="date"
            value={form.expectedDate ?? ''}
            onChange={(event) => setForm({ ...form, expectedDate: event.target.value || null })}
          />
        </label>

        <label>
          Monto exacto
          <input
            className="input-ui"
            type="number"
            placeholder="Monto exacto"
            value={form.amount ?? ''}
            onChange={(event) => setForm({ ...form, amount: toNullableNumber(event.target.value) })}
          />
        </label>

        <label>
          Prioridad
          <select
            className="input-ui"
            value={form.priority}
            onChange={(event) =>
              setForm({ ...form, priority: event.target.value as MonthlyPlanItem['priority'] })
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

      {error ? <p className="mensaje-error">{error}</p> : null}

      <div className="form-actions">
        <button
          type="button"
          className="boton-principal"
          onClick={onCreate}
          disabled={!form.title.trim() || isCreating}
        >
          {isCreating ? 'Creando...' : 'Crear ítem'}
        </button>
      </div>
    </section>
  );
}