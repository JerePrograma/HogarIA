import type { HTMLAttributes } from "react";
import { classNames } from "../lib/classNames";

export function Surface({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div {...props} className={classNames("surface-inset", className)} />;
}
