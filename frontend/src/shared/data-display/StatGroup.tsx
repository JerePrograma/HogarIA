import type { ReactNode } from "react";

type StatItem = {
  label: string;
  value: ReactNode;
};

type StatGroupProps = {
  items: StatItem[];
};

export function StatGroup({ items }: StatGroupProps) {
  return (
    <div className="metric-grid">
      {items.map((item) => (
        <article key={item.label} className="surface-inset">
          <p className="label-ui">{item.label}</p>
          <strong>{item.value}</strong>
        </article>
      ))}
    </div>
  );
}
