import type { HTMLAttributes } from "react";
import { classNames } from "../lib/classNames";

export function MobileCardList({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div {...props} className={classNames("mobile-card-list", className)} />;
}
