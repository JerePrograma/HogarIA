import type { HTMLAttributes, ReactNode } from "react";
import { classNames } from "../lib/classNames";

type PageProps = HTMLAttributes<HTMLDivElement> & {
  children: ReactNode;
};

export function Page({ className, ...props }: PageProps) {
  return <div {...props} className={classNames("page-stack", className)} />;
}
