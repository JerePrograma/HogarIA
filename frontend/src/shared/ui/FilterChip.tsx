import type { ButtonHTMLAttributes } from "react";
import { classNames } from "../lib/classNames";

type FilterChipProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  active?: boolean;
};

export function FilterChip({ active = false, className, ...props }: FilterChipProps) {
  return (
    <button
      type="button"
      {...props}
      className={classNames("filter-chip", active && "active", className)}
    />
  );
}
