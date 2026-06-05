interface Props {
  canCommit: boolean;
  pending: boolean;
  hasMissingCategory: boolean;
  onCommit: () => void;
}

export function ImportCommitPanel({
  canCommit,
  pending,
  hasMissingCategory,
  onCommit,
}: Props) {
  return (
    <section className="import-commit-panel" aria-live="polite">
      <div className="import-commit-copy">
        <p className="eyebrow">Confirmación</p>
        <h3>Aplicar importación</h3>

        {hasMissingCategory ? (
          <p className="mensaje-warning">
            Hay filas NEEDS_CATEGORY sin categoría. Asigná una categoría o activá
            una categoría temporal compatible antes de confirmar.
          </p>
        ) : (
          <p className="muted">
            Hay filas en revisión. Las técnicas/neutras se importarán como revisión
            o transferencia técnica; las filas NEEDS_CATEGORY requieren categoría o fallback.
          </p>
        )}
      </div>

      <div className="import-commit-actions">
        <button
          type="button"
          className="boton-principal"
          disabled={!canCommit}
          onClick={onCommit}
        >
          {pending ? 'Confirmando importación...' : 'Confirmar importación'}
        </button>

        {!canCommit && !pending ? (
          <span className="muted">
            La importación todavía no está lista para confirmar.
          </span>
        ) : null}
      </div>
    </section>
  );
}
