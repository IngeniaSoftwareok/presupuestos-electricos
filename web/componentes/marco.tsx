"use client";

import { ClipboardList, FlaskConical, History, Zap } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { modoNavegador } from "../lib/acceso";

export function Marco({ children }: { children: ReactNode }) {
  const ruta = usePathname();
  const enHistorial = ruta?.startsWith("/historial");

  return (
    <div className="aplicacion">
      <a className="salto" href="#contenido">Saltar al contenido</a>
      <aside className="lateral">
        <Link className="marca" href="/">
          <span className="marca-icono"><Zap size={20} fill="currentColor" aria-hidden="true" /></span>
          <span><strong>Presupuestos</strong><small>Instalaciones eléctricas</small></span>
        </Link>
        <nav className="navegacion" aria-label="Principal">
          <Link href="/" aria-current={!enHistorial ? "page" : undefined}>
            <ClipboardList size={19} aria-hidden="true" /> Nuevo presupuesto
          </Link>
          <Link href="/historial/" aria-current={enHistorial ? "page" : undefined}>
            <History size={19} aria-hidden="true" /> Historial
          </Link>
        </nav>
        <div className="nota-demo">
          <FlaskConical size={17} aria-hidden="true" />
          <div>
            <strong>Datos de ejemplo</strong>
            <p>{modoNavegador ? "El historial se guarda en este navegador." : "Tu historial queda asociado a este navegador."}</p>
          </div>
        </div>
      </aside>
      <main id="contenido" className="contenido" tabIndex={-1}>{children}</main>
    </div>
  );
}
