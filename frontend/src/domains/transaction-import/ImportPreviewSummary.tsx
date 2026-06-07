interface Props {
  totalRows: number;
  importableRows: number;
  duplicateRows: number;
  invalidRows: number;
  blockedRows: number;
  blockingCategoryRows: number;
  internalTransferRows: number;
  reviewRows?: number;
  needsCategoryRows?: number;
  technicalNeutralRows?: number;
  suggestedCategoryRows?: number;
}

function SummaryItem({
  title,
  value,
  helper,
  tone,
}: {
  title: string;
  value: number;
  helper: string;
  tone: 'ok' | 'warning' | 'danger' | 'neutral' | 'info';
}) {
  return (
    <article className={`import-preview-item import-preview-item-${tone}`}>
      <span>{title}</span>
      <strong>{value}</strong>
      <p>{helper}</p>
    </article>
  );
}

export function ImportPreviewSummary({
  totalRows,
  importableRows,
  duplicateRows,
  invalidRows,
  blockedRows,
  blockingCategoryRows,
  internalTransferRows,
  reviewRows = 0,
  needsCategoryRows = 0,
  technicalNeutralRows = 0,
  suggestedCategoryRows = 0,
}: Props) {
  return (
    <section className="import-preview-summary" aria-label="Resumen de previsualización">
      <SummaryItem
        title="Total"
        value={totalRows}
        helper="Filas detectadas en el archivo."
        tone="info"
      />

      <SummaryItem
        title="Importables"
        value={importableRows}
        helper="Filas listas para confirmar."
        tone="ok"
      />

      <SummaryItem
        title="Duplicadas"
        value={duplicateRows}
        helper="Se omiten para evitar repetidos."
        tone="neutral"
      />

      <SummaryItem
        title="Sugeridas"
        value={suggestedCategoryRows}
        helper="Tienen categoría candidata."
        tone="info"
      />

      <SummaryItem
        title="Necesitan categoría"
        value={needsCategoryRows}
        helper="Filas detectadas como pendientes de categoría."
        tone={needsCategoryRows > 0 ? 'warning' : 'neutral'}
      />

      <SummaryItem
        title="Bloqueadas por categoría"
        value={blockingCategoryRows}
        helper="No tienen una decisión de categoría compatible."
        tone={blockingCategoryRows > 0 ? 'warning' : 'neutral'}
      />

      <SummaryItem
        title="A revisar"
        value={reviewRows}
        helper="No siempre bloquean; necesitan criterio antes del cierre."
        tone={reviewRows > 0 ? 'warning' : 'neutral'}
      />

      <SummaryItem
        title="Transferencias"
        value={internalTransferRows}
        helper="Internas/técnicas, sin impacto operativo."
        tone={internalTransferRows > 0 ? 'info' : 'neutral'}
      />

      <SummaryItem
        title="Técnicas/neutras"
        value={technicalNeutralRows}
        helper="No impactan ingresos/gastos operativos."
        tone={technicalNeutralRows > 0 ? 'info' : 'neutral'}
      />

      <SummaryItem
        title="No importables ahora"
        value={blockedRows}
        helper="Filas que no se crearán con la decisión actual."
        tone={blockedRows > 0 ? 'danger' : 'neutral'}
      />

      <SummaryItem
        title="Error de archivo"
        value={invalidRows}
        helper="No se van a importar."
        tone={invalidRows > 0 ? 'danger' : 'neutral'}
      />
    </section>
  );
}
