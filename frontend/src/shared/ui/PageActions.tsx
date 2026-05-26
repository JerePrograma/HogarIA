import type { HTMLAttributes } from "react";
import { classNames } from "../lib/classNames";

export function PageActions({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div {...props} className={classNames("page-actions", className)} />;
}
