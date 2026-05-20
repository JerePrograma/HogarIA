import type { FormEvent } from "react";
import { ErrorState } from "../../../components/ui/ErrorState";
import {
  labelOrValue,
  movementTypeLabels,
  transactionStatusLabels,
} from "../../../domain/financeLabels";
import {
  currencyOptions,
  movementTypeOptions,
  transactionStatusOptions,
} from "../../../domain/financeOptions";
import { formatMoney } from "../../../domain/formatters";
import type {
  Account,
  Category,
  MovementType,
  TransactionStatus,
} from "../../../domain/types";
import type { TransactionForm } from "../types";
import { FilterChip } from "./FilterChip";

interface Props {
  form: TransactionForm;
  accounts: Account[];
  compatibleCategories: Category[];
  canSave: boolean;
  pending: boolean;
  isError: boolean;
  onFormChange: (patch: Partial<TransactionForm>) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}

export function TransactionQuickForm({
  form,
  accounts,
  compatibleCategories,
  canSave,
  pending,
  isError,
  onFormChange,
  onSubmit,
}: Props) {
  return (
    <form className="panel transactions-form-panel" onSubmit={onSubmit}>
      <div className="section-title">
        <div>
          <p className="eyebrow">Alta rápida</p>
          <h2>Cargar movimiento</h2>
          <p className="muted">
            Mantené cuenta, categoría y tipo para cargar varios movimientos
            seguidos.
          </p>
        </div>
      </div>

      <div className="transactions-type-picker">
        {movementTypeOptions.map((option) => (
          <FilterChip
            key={option.value}
            active={form.movementType === option.value}
            onClick={() =>
              onFormChange({
                movementType: option.value as MovementType,
                categoryId: "",
              })
            }
          >
            {option.label}
          </FilterChip>
        ))}
      </div>

      <div className="form-grid">
        <label>
          Cuenta
          <select
            className="input-ui"
            value={form.accountId}
            onChange={(event) =>
              onFormChange({ accountId: event.target.value })
            }
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
            onChange={(event) =>
              onFormChange({ categoryId: event.target.value })
            }
          >
            <option value="">Seleccionar categoría</option>
            {compatibleCategories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.technical
                  ? `${category.name} · técnica`
                  : category.name}
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
              onFormChange({
                status: event.target.value as TransactionStatus,
              })
            }
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
            onChange={(event) => onFormChange({ currency: event.target.value })}
          >
            {currencyOptions.map((currency) => (
              <option key={currency.value} value={currency.value}>
                {currency.label}
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
            onChange={(event) => onFormChange({ realDate: event.target.value })}
          />
        </label>

        <label>
          Fecha de presupuesto
          <input
            className="input-ui"
            type="date"
            value={form.budgetDate}
            onChange={(event) =>
              onFormChange({ budgetDate: event.target.value })
            }
          />
        </label>

        <label>
          Monto
          <input
            className="input-ui"
            type="number"
            min={0}
            step="0.01"
            value={form.amount === 0 ? "" : form.amount}
            placeholder="0,00"
            onChange={(event) =>
              onFormChange({
                amount: Number(event.target.value),
              })
            }
          />
        </label>

        <label className="form-field-wide">
          Descripción
          <input
            className="input-ui"
            value={form.description}
            placeholder="Ej: supermercado, sueldo, alquiler"
            onChange={(event) =>
              onFormChange({ description: event.target.value })
            }
          />
        </label>
      </div>

      <div className="transactions-form-preview">
        <span className="label-ui">Vista previa</span>
        <strong>
          {form.amount > 0 ? formatMoney(form.amount, form.currency) : "$ 0,00"}
        </strong>
        <p className="muted">
          {labelOrValue(movementTypeLabels, form.movementType)} ·{" "}
          {labelOrValue(transactionStatusLabels, form.status)}
        </p>
      </div>

      <div className="form-actions">
        <button type="submit" className="boton-principal" disabled={!canSave}>
          {pending ? "Guardando..." : "Guardar movimiento"}
        </button>

        {!canSave ? (
          <span className="muted">
            Completá cuenta, categoría, monto y fechas para guardar.
          </span>
        ) : null}
      </div>

      {isError ? (
        <ErrorState message="No se pudo guardar el movimiento. Revisá los datos ingresados." />
      ) : null}
    </form>
  );
}
