import type { ReactNode } from "react";

type FieldProps = {
  label: string;
  helper?: string;
  error?: string;
  children: ReactNode;
  className?: string;
};

export function Field({ label, helper, error, children, className }: FieldProps) {
  return (
    <label className={className}>
      {label}
      {children}
      {helper ? <span className="compact-muted">{helper}</span> : null}
      {error ? <span className="compact-error">{error}</span> : null}
    </label>
  );
}
