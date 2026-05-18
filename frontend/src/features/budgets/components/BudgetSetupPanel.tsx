type Props = {
  year: number;
  month: number;
  loading: boolean;
  pending: boolean;
  onPreparePeriod: () => void;
};

export function BudgetSetupPanel({
  year,
  month,
  loading,
  pending,
  onPreparePeriod,
}: Props) {
  return (
    <section className="panel">
      <div className="section-title">
        <div>
          <p className="eyebrow">Configuración inicial</p>
          <h2>Período presupuestario no preparado</h2>
          <p className="secondary-text">
            Para cargar importes y comparar contra movimientos reales, primero
            prepará el período seleccionado.
          </p>
        </div>
      </div>

      <div className="empty-state">
        <h3>
          Presupuesto {month}/{year}
        </h3>

        <p>
          Se creará la estructura necesaria para cargar topes por categoría.
        </p>

        <button
          type="button"
          className="boton-principal"
          onClick={onPreparePeriod}
          disabled={loading || pending}
        >
          {pending ? "Preparando período..." : "Preparar período"}
        </button>
      </div>
    </section>
  );
}