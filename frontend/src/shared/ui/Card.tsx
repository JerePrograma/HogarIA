import type { HTMLAttributes } from "react";
import { classNames } from "../lib/classNames";

type CardProps = HTMLAttributes<HTMLElement> & {
  as?: "article" | "section" | "div";
  variant?: "default" | "soft" | "muted" | "accent";
};

const variantClass = {
  default: "panel",
  soft: "panel-soft",
  muted: "panel-muted",
  accent: "panel-accent",
} as const;

export function Card({
  as: Component = "article",
  variant = "default",
  className,
  ...props
}: CardProps) {
  return <Component {...props} className={classNames(variantClass[variant], className)} />;
}
