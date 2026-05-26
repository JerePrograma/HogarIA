import type { HTMLAttributes } from "react";
import { classNames } from "../lib/classNames";

export function FormGrid({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div {...props} className={classNames("form-grid", className)} />;
}

export function FormActions({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div {...props} className={classNames("form-actions", className)} />;
}
