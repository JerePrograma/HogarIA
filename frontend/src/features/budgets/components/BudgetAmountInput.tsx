import { useEffect, useState } from "react";

type Props = {
  value: number;
  disabled?: boolean;
  saving?: boolean;
  onCommit: (value: number) => void;
};

export function BudgetAmountInput({
  value,
  disabled = false,
  saving = false,
  onCommit,
}: Props) {
  const [draftValue, setDraftValue] = useState(String(value));
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setDraftValue(String(value));
  }, [value]);

  const commit = () => {
    const trimmedValue = draftValue.trim();
    const nextAmount = trimmedValue === "" ? 0 : Number(trimmedValue);

    if (!Number.isFinite(nextAmount) || nextAmount < 0) {
      setError("Importe inválido");
      setDraftValue(String(value));
      return;
    }

    setError(null);

    if (nextAmount === value) {
      return;
    }

    onCommit(nextAmount);
  };

  return (
    <div className="stack-ui items-end">
      <div className="flex items-center justify-end gap-2">
        <input
          className="input-ui"
          type="number"
          min={0}
          step="0.01"
          value={draftValue}
          disabled={disabled}
          onChange={(event) => setDraftValue(event.currentTarget.value)}
          onBlur={commit}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.currentTarget.blur();
            }
          }}
        />

        {saving ? <span className="secondary-text">Guardando...</span> : null}
      </div>

      {error ? <span className="mensaje-error">{error}</span> : null}
    </div>
  );
}
