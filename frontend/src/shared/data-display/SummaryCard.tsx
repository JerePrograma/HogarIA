import type { ReactNode } from "react";
import { Card } from "../ui/Card";

type SummaryCardProps = {
  label: string;
  value: ReactNode;
  helper?: string;
};

export function SummaryCard({ label, value, helper }: SummaryCardProps) {
  return (
    <Card variant="muted">
      <p className="label-ui">{label}</p>
      <strong>{value}</strong>
      {helper ? <p className="muted">{helper}</p> : null}
    </Card>
  );
}
