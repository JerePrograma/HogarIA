interface Props {
  totalRows: number;
  importableRows: number;
  duplicateRows: number;
  invalidRows: number;
  ignoredRows: number;
}

export function ImportPreviewSummary({ totalRows, importableRows, duplicateRows, invalidRows, ignoredRows }: Props) {
  return (
    <p className="muted">
      Total: {totalRows} · Importables: {importableRows} · Duplicadas: {duplicateRows} · Con error: {invalidRows} · Ignoradas: {ignoredRows}
    </p>
  );
}
