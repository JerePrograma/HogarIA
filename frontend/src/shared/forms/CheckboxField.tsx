import type { InputHTMLAttributes, ReactNode } from "react";

type CheckboxFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  children: ReactNode;
};

export function CheckboxField({ children, ...props }: CheckboxFieldProps) {
  return (
    <label className="surface-inset cluster-ui">
      <input {...props} type="checkbox" />
      <span>{children}</span>
    </label>
  );
}
