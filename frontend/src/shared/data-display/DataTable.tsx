import type { ReactNode } from "react";

type DataTableProps = {
  children: ReactNode;
  minWidth?: number;
};

export function DataTable({ children, minWidth }: DataTableProps) {
  return (
    <div className="tabla-ui">
      <table className="table-compact" style={minWidth ? { minWidth } : undefined}>
        {children}
      </table>
    </div>
  );
}
