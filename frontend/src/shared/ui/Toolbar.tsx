import type { HTMLAttributes } from "react";
import { classNames } from "../lib/classNames";

export function Toolbar({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div {...props} className={classNames("toolbar-ui", className)} />;
}
