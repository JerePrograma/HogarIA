import { NavLink } from "react-router-dom";

export type TabItem = {
  label: string;
  to: string;
  end?: boolean;
};

type TabsProps = {
  items: TabItem[];
  ariaLabel: string;
};

export function Tabs({ items, ariaLabel }: TabsProps) {
  return (
    <nav className="planning-filter-row" aria-label={ariaLabel}>
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end}
          className={({ isActive }) =>
            `planning-filter-chip ${isActive ? "active" : ""}`.trim()
          }
        >
          {item.label}
        </NavLink>
      ))}
    </nav>
  );
}
