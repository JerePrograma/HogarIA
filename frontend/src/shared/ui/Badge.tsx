import type { HTMLAttributes } from "react";
import { classNames } from "../lib/classNames";

type BadgeTone = "neutral" | "info" | "success" | "warning" | "danger";

const toneClass: Record<BadgeTone, string> = {
  neutral: "badge-ui badge-muted",
  info: "badge-ui badge-info",
  success: "badge-ui badge-success",
  warning: "badge-ui badge-warning",
  danger: "badge-ui badge-danger",
};

type BadgeProps = HTMLAttributes<HTMLSpanElement> & {
  tone?: BadgeTone;
};

export function Badge({ tone = "neutral", className, ...props }: BadgeProps) {
  return <span {...props} className={classNames(toneClass[tone], className)} />;
}
