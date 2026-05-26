import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Link, type LinkProps } from "react-router-dom";
import { classNames } from "../lib/classNames";

type ButtonTone = "primary" | "secondary" | "ghost" | "danger";

type BaseProps = {
  tone?: ButtonTone;
  fullWidth?: boolean;
  children: ReactNode;
};

type NativeButtonProps = BaseProps &
  ButtonHTMLAttributes<HTMLButtonElement> & {
    to?: never;
  };

type LinkButtonProps = BaseProps &
  LinkProps & {
    to: string;
  };

type ButtonProps = NativeButtonProps | LinkButtonProps;

const toneClass: Record<ButtonTone, string> = {
  primary: "boton-principal",
  secondary: "boton-secundario",
  ghost: "boton-fantasma",
  danger: "boton-danger",
};

export function Button({
  tone = "primary",
  fullWidth = false,
  className,
  children,
  ...props
}: ButtonProps) {
  const classes = classNames(toneClass[tone], fullWidth && "w-full", className);

  if ("to" in props && props.to) {
    const linkProps = props as Omit<LinkButtonProps, keyof BaseProps>;

    return (
      <Link {...linkProps} className={classes}>
        {children}
      </Link>
    );
  }

  const buttonProps = props as Omit<NativeButtonProps, keyof BaseProps>;

  return (
    <button {...buttonProps} className={classes}>
      {children}
    </button>
  );
}
