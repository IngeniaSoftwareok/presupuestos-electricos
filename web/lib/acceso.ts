import { ErrorDemo, type AccesoPresupuestos, type Campos, type Datos } from "./tipos";

type ProblemaHttp = { detail?: string; codigo?: string; campos?: Campos };

async function solicitar<T>(ruta: string, opciones?: RequestInit): Promise<T> {
  const respuesta = await fetch(`/api${ruta}`, { cache: "no-store", ...opciones });
  if (!respuesta.ok) {
    const problema: ProblemaHttp | null = await respuesta.json().catch(() => null);
    if (problema?.codigo === "ESPACIO_REQUERIDO") datosPendientes = undefined;
    throw new ErrorDemo(problema?.detail ?? "No pude completar la operación. Volvé a intentar.",
      respuesta.status, problema?.campos);
  }
  return respuesta.json();
}

const servidor: AccesoPresupuestos = {
  datos: () => solicitar("/datos"),
  guardar: (pedido, clave) => solicitar("/presupuestos", {
    method: "POST",
    headers: { "Content-Type": "application/json", "Idempotency-Key": clave },
    body: JSON.stringify(pedido),
  }),
  historial: (antesDe) => solicitar(antesDe ? `/presupuestos?antesDe=${encodeURIComponent(antesDe)}` : "/presupuestos"),
  detalle: (id) => solicitar(`/presupuestos/${encodeURIComponent(id)}`),
};

export const modoNavegador = process.env.NEXT_PUBLIC_MODO_DEMO === "navegador";
let datosPendientes: Promise<Datos> | undefined;

async function acceso(): Promise<AccesoPresupuestos> {
  if (modoNavegador) return (await import("./navegador")).navegador;
  return servidor;
}

export const demo: AccesoPresupuestos = {
  datos() {
    datosPendientes ??= acceso().then((fuente) => fuente.datos()).catch((error: unknown) => {
      datosPendientes = undefined;
      throw error;
    });
    return datosPendientes;
  },
  async guardar(pedido, clave) {
    await demo.datos();
    return (await acceso()).guardar(pedido, clave);
  },
  async historial(antesDe) {
    await demo.datos();
    return (await acceso()).historial(antesDe);
  },
  async detalle(id) {
    await demo.datos();
    return (await acceso()).detalle(id);
  },
};
