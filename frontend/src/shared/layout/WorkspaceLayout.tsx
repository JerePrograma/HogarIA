import type { ReactNode } from "react";
import { AppShell } from "../../app/shell/AppShell";
import { Page } from "./Page";

type WorkspaceLayoutProps = {
  children: ReactNode;
  className?: string;
};

export function WorkspaceLayout({ children, className }: WorkspaceLayoutProps) {
  return (
    <AppShell>
      <Page className={className}>{children}</Page>
    </AppShell>
  );
}
