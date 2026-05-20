import type { Account, Category, MovementType, TransactionStatus } from '../../../domain/types';
import { movementTypeOptions, transactionStatusOptions } from '../../../domain/financeOptions';
import { movementTypeLabels, transactionStatusLabels, labelOrValue } from '../../../domain/financeLabels';
import { getCompatibleCategories } from '../../../domain/transactionRules';
import { formatMoney } from '../../../domain/formatters';

export interface TransactionForm {
  accountId: string;
  categoryId: string;
  movementType: MovementType;
  realDate: string;
  budgetDate: string;
  amount: number;
  currency: string;
  description: string;
  status: TransactionStatus;
}

interface Props {
  form: TransactionForm;
  accounts: Account[];
  categories: Category[];
  pending: boolean;
  canSave: boolean;
  onChange: (patch: Partial<TransactionForm>) => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}

const currencyOptions = ['ARS', 'USD'];

export function TransactionQuickForm({
  form,
  accounts,
  categories,
  pending,
  canSave,
  onChange,
  onSubmit,
}: Props) {
  const compatibleCategories = getCompatibleCategories(categories, form.movementType, {
    includeTechnical: false,
  });

  return (
    <form className="panel transactions-form-panel" onSubmit={onSubmit}>
      <div className="section-title">
        <div>
          <p className="eyebrow">Alta rápida</p>
          <h2>Cargar movimiento</h2>
          <p className="muted">
            Mantené cuenta, categoría y tipo para cargar varios movimientos seguidos.
          </p>
        </div>
      </div>

      <div className="transactions-type-picker">
        {movementTypeOptions.map((option) => (
          <button
            key={option.value}
            type="button"
            className={`tx-filter-chip ${form.movementType === option.value ? 'active' : ''}`}
            onClick={() =>
              onChange({
                movementType: option.value as MovementType,
                categoryId: '',
              })
            }
          >
            {option.label}
          </button>
        ))}
      </div>

      <div className="form-grid">
        <label>
          Cuenta
          <select
            className="input-ui"
            value={form.accountId}
            onChange={(event) => onChange({ accountId: event.target.value })}
          >
            <option value="">Seleccionar cuenta</option>
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
            value={form.categoryId}
            onChange={(event) => onChange({ categoryId: event.target.value })}
          >
            <option value="">Seleccionar categoría</option>
            {compatibleCategories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          Estado
          <select
            className="input-ui"
            value={form.status}
            onChange={(event) => onChange({ status: event.target.value as TransactionStatus })}
          >
            {transactionStatusOptions.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <label>
          Moneda
          <select
            className="input-ui"
            value={form.currency}
            onChange={(event) => onChange({ currency: event.target.value })}
          >
            {currencyOptions.map((currency) => (
              <option key={currency} value={currency}>
                {currency}
              </option>
            ))}
          </select>
        </label>

        <label>
          Fecha real
          <input
            className="input-ui"
            type="date"
            value={form.realDate}
            onChange={(event) => onChange({ realDate: event.target.value })}
          />
        </label>

        <label>
          Fecha de presupuesto
          <input
            className="input-ui"
            type="date"
            value={form.budgetDate}
            onChange={(event) => onChange({ budgetDate: event.target.value })}
          />
        </label>

        <label>
          Monto
          <input
            className="input-ui"
            type="number"
            min={0}
            step="0.01"
            value={form.amount === 0 ? '' : form.amount}
            placeholder="0,00"
            onChange={(event) => onChange({ amount: Number(event.target.value) })}
          />
        </label>

        <label className="form-field-wide">
          Descripción
          <input
            className="input-ui"
            value={form.description}
            placeholder="Ej: supermercado, sueldo, alquiler"
            onChange={(event) => onChange({ description: event.target.value })}
          />
        </label>
      </div>

      <div className="transactions-form-preview">
        <span className="label-ui">Vista previa</span>
        <strong>{form.amount > 0 ? formatMoney(form.amount, form.currency) : '$ 0,00'}</strong>
        <p className="muted">
          {labelOrValue(movementTypeLabels, form.movementType)} ·{' '}
          {labelOrValue(transactionStatusLabels, form.status)}
        </p>
      </div>

      <div className="form-actions">
        <button type="submit" className="boton-principal" disabled={!canSave}>
          {pending ? 'Guardando...' : 'Guardar movimiento'}
        </button>

        {!canSave ? (
          <span className="muted">
            Completá cuenta, categoría compatible, monto y fechas para guardar.
          </span>
        ) : null}
      </div>
    </form>
  );
}