import type { ReactNode } from "react";
import { Card } from "../ui/Card";
import { SectionTitle } from "../ui/SectionTitle";

type PageSectionProps = {
  eyebrow?: string;
  title?: string;
  description?: string;
  action?: ReactNode;
  children: ReactNode;
  compact?: boolean;
};

export function PageSection({
  eyebrow,
  title,
  description,
  action,
  children,
  compact = false,
}: PageSectionProps) {
  return (
    <Card as="section" className={compact ? "h-full" : undefined}>
      {title ? (
        <SectionTitle
          eyebrow={eyebrow}
          title={title}
          description={description}
          action={action}
        />
      ) : null}
      {children}
    </Card>
  );
}
