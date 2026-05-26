import { EmptyState } from "../ui/EmptyState";

type ChartEmptyStateProps = {
  message?: string;
};

export function ChartEmptyState({
  message = "Todavía no hay datos suficientes para graficar.",
}: ChartEmptyStateProps) {
  return <EmptyState title="Sin datos para graficar" message={message} />;
}
