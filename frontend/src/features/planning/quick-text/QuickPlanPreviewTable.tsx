import type { QuickPlanCandidate } from "../../../api/quickPlanTextImportApi";

type Props = {
  rows: QuickPlanCandidate[];
  setRows: (rows: QuickPlanCandidate[]) => void;
};

const typeOptions = [
  { value: "INCOME", label: "Ingreso" },
  { value: "EXPENSE", label: "Egreso" },
  { value: "SAVING", label: "Ahorro" },
  { value: "DEBT", label: "Deuda" },
  { value: "RECOVERY", label: "Recupero" },
  { value: "TODO", label: "Tarea" },
];

const priorityOptions = [
  { value: "ESSENTIAL", label: "Esencial" },
  { value: "IMPORTANT", label: "Importante" },
  { value: "OPTIONAL", label: "Opcional" },
];

export function QuickPlanPreviewTable({ rows, setRows }: Props) {
  const updateCandidate = (
    index: number,
    key: string,
    value: string | number | null,
  ) => {
    setRows(
      rows.map((row, currentIndex) =>
        currentIndex === index
          ? {
              ...row,
              candidate: {
                ...row.candidate,
                [key]: value,
              },
            }
          : row,
      ),
    );
  };

  const removeRow = (index: number) => {
    setRows(rows.filter((_, currentIndex) => currentIndex !== index));
  };

  return (
    <div className="tabla-ui">
      <table className="table-compact">
        <thead>
          <tr>
            <th>Línea</th>
            <th>Título</th>
            <th>Tipo</th>
            <th className="amount-cell">Monto</th>
            <th>Rango</th>
            <th>Prioridad</th>
            <th>Categoría</th>
            <th>Estado</th>
            <th>Advertencias</th>
            <th></th>
          </tr>
        </thead>

        <tbody>
          {rows.map((row, index) => {
            const candidate = row.candidate;

            return (
              <tr key={`${row.lineNumber}-${row.rawLine}`}>
                <td>
                  <strong>#{row.lineNumber}</strong>
                  <p className="compact-muted">{row.rawLine}</p>
                </td>

                <td>
                  <input
                    className="input-ui"
                    value={candidate.title ?? ""}
                    onChange={(event) =>
                      updateCandidate(index, "title", event.target.value)
                    }
                  />
                </td>

                <td>
                  <select
                    className="input-ui"
                    value={candidate.type}
                    onChange={(event) =>
                      updateCandidate(index, "type", event.target.value)
                    }
                  >
                    {typeOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </td>

                <td className="amount-cell">
                  <input
                    className="input-ui"
                    type="number"
                    step="0.01"
                    value={candidate.amount ?? ""}
                    onChange={(event) =>
                      updateCandidate(
                        index,
                        "amount",
                        event.target.value ? Number(event.target.value) : null,
                      )
                    }
                  />
                </td>

                <td>
                  {candidate.minAmount || candidate.maxAmount ? (
                    <span>
                      {candidate.minAmount ?? "-"} -{" "}
                      {candidate.maxAmount ?? "-"}
                    </span>
                  ) : (
                    <span className="muted">-</span>
                  )}
                </td>

                <td>
                  <select
                    className="input-ui"
                    value={candidate.priority}
                    onChange={(event) =>
                      updateCandidate(index, "priority", event.target.value)
                    }
                  >
                    {priorityOptions.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </td>

                <td>
                  {row.suggestedCategoryName ? (
                    <span>{row.suggestedCategoryName}</span>
                  ) : (
                    <span className="muted">Sin categoría</span>
                  )}
                </td>

                <td>
                  {row.duplicate ? (
                    <span className="badge badge-warning">Duplicado</span>
                  ) : (
                    <span className="badge badge-ok">Nuevo</span>
                  )}
                </td>

                <td>
                  {row.warnings.length > 0 ? (
                    <ul className="mb-0">
                      {row.warnings.map((warning, warningIndex) => (
                        <li key={warningIndex}>{warning}</li>
                      ))}
                    </ul>
                  ) : (
                    <span className="muted">Sin advertencias</span>
                  )}
                </td>

                <td>
                  <button
                    type="button"
                    className="boton-fantasma"
                    onClick={() => removeRow(index)}
                  >
                    Quitar
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
