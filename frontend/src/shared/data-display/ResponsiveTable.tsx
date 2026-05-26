import type { ReactNode } from "react";

type ResponsiveTableProps = {
  table: ReactNode;
  mobile: ReactNode;
};

export function ResponsiveTable({ table, mobile }: ResponsiveTableProps) {
  return (
    <>
      <div className="responsive-table-desktop">{table}</div>
      <div className="responsive-table-mobile">{mobile}</div>
    </>
  );
}
