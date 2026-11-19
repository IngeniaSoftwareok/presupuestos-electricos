import { AlertCircle, ChevronDown, LoaderCircle } from "lucide-react";
import type { ReactNode, SelectHTMLAttributes } from "react";

export function Selector({ children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <div className="selector">
      <select {...props}>{children}</select>
      <ChevronDown size={17} aria-hidden="true" />
    </div>
  );
}

export function Aviso({ children }: { children: ReactNode }) {
  return <div className="aviso" role="alert"><AlertCircle size={18} aria-hidden="true" /><div>{children}</div></div>;
}

export function Cargando({ children }: { children: ReactNode }) {
  return <div className="estado" role="status"><LoaderCircle className="giro" size={19} aria-hidden="true" />{children}</div>;
}

export function ErrorCampo({ id, mensaje }: { id: string; mensaje?: string }) {
  return mensaje ? <span id={id} className="error-campo">{mensaje}</span> : null;
}
