import type { Metadata } from "next";
import type { ReactNode } from "react";
import "./estilos.css";
import { Marco } from "../componentes/marco";

export const metadata: Metadata = {
  title: { default: "Presupuestos eléctricos", template: "%s · Presupuestos eléctricos" },
  description: "Armá un presupuesto de instalación eléctrica y volvé a abrirlo desde el historial.",
};

export default function Layout({ children }: { children: ReactNode }) {
  return (
    <html lang="es-AR">
      <body>
        <Marco>{children}</Marco>
      </body>
    </html>
  );
}
