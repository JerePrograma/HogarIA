interface Props {
  canCommit: boolean;
  pending: boolean;
  hasMissingCategory: boolean;
  onCommit: () => void;
}

export function ImportCommitPanel({ canCommit, pending, hasMissingCategory, onCommit }: Props) {
  return (
    <>
      {hasMissingCategory ? <p className="mensaje-warning">Hay gastos listos para importar sin categoría.</p> : null}
      <button type="button" disabled={!canCommit} onClick={onCommit}>
        {pending ? 'Confirmando importación…' : 'Confirmar importación'}
      </button>
    </>
  );
}
