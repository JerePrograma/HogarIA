import type { ReactNode } from "react";
import { classNames } from "../lib/classNames";

type AlertTone = "info" | "success" | "warning" | "error";

const toneClass: Record<AlertTone, string> = {
  info: "mensaje-info",
  success: "mensaje-exito",
  warning: "mensaje-warning",
  error: "mensaje-error",
};

type AlertProps = {
  tone?: AlertTone;
  title?: string;
  children: ReactNode;
  className?: string;
};

export function Alert({ tone = "info", title, children, className }: AlertProps) {
  return (
    <div className={classNames(toneClass[tone], className)} role={tone === "error" ? "alert" : "status"}>
      {title ? <strong>{title}</strong> : null}
      <p className="m-0">{children}</p>
    </div>
  );
}
