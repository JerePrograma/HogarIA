import { Tabs, type TabItem } from "../ui/Tabs";

type WorkspaceTabsProps = {
  items: TabItem[];
  ariaLabel: string;
};

export function WorkspaceTabs({ items, ariaLabel }: WorkspaceTabsProps) {
  return <Tabs items={items} ariaLabel={ariaLabel} />;
}
