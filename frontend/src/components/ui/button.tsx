import type { ButtonHTMLAttributes, ReactNode } from "react";

type ButtonVariant = "primary" | "secondary" | "text" | "danger";

const variantClasses: Record<ButtonVariant, string> = {
  primary: "rounded-md bg-rose-700 px-4 py-2 text-sm font-medium text-white hover:bg-rose-800 disabled:bg-stone-300",
  secondary:
    "rounded-md border border-stone-300 bg-white px-4 py-2 text-sm font-medium text-stone-800 hover:bg-stone-50 disabled:text-stone-400",
  text: "px-0 py-2 text-sm font-medium text-stone-500 hover:text-stone-900 disabled:text-stone-400",
  danger: "rounded-md bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800 disabled:bg-stone-300",
};

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant;
  children: ReactNode;
};

export function Button({ variant = "primary", className = "", children, ...props }: ButtonProps) {
  return (
    <button
      className={`${variantClasses[variant]} min-h-11 transition-colors disabled:cursor-not-allowed ${className}`.trim()}
      {...props}
    >
      {children}
    </button>
  );
}
