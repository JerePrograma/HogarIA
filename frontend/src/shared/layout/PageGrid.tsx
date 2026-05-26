import type { HTMLAttributes } from "react";
import { classNames } from "../lib/classNames";

export function PageGrid({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div {...props} className={classNames("grid gap-4", className)} />;
}
