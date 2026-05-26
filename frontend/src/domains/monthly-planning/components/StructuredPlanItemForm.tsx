import type {
  Account,
  Category,
  MonthlyPlanItemCreatePayload,
} from '../../../domain/types';
import { PlanItemFormFields } from './PlanItemFormFields';

type Props = {
  form: MonthlyPlanItemCreatePayload;
  setForm: (form: MonthlyPlanItemCreatePayload) => void;
  accounts: Account[];
  categories: Category[];
  onCreate: () => void;
  isCreating?: boolean;
  error?: string | null;
};

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

      <PlanItemFormFields
        form={form}
        setForm={setForm}
        accounts={accounts}
        categories={categories}
        compact
      />

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