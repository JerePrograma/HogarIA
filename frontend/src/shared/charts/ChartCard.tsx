import type { ReactNode } from "react";
import { PageSection } from "../layout/PageSection";

type ChartCardProps = {
  title: string;
  eyebrow?: string;
  description?: string;
  children: ReactNode;
};

export function ChartCard({ title, eyebrow, description, children }: ChartCardProps) {
  return (
    <PageSection title={title} eyebrow={eyebrow} description={description}>
      <div className="chart-card">{children}</div>
    </PageSection>
  );
}
