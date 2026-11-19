import type { Metadata } from "next";
import { Suspense } from "react";
import { Historial } from "../../componentes/historial";

export const metadata: Metadata = { title: "Historial" };

export default function PaginaHistorial() {
  return (
    <Suspense fallback={<div className="estado" role="status">Cargando historial…</div>}>
      <Historial />
    </Suspense>
  );
}
