import type { ReactNode } from "react";

interface Props {
  active: boolean;
  children: ReactNode;
  onClick: () => void;
}

export function FilterChip({ active, children, onClick }: Props) {
  return (
    <button
      type="button"
      className={`tx-filter-chip ${active ? "active" : ""}`}
      onClick={onClick}
    >
      {children}
    </button>
  );
}
