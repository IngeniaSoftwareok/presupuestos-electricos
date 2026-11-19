"use client";

import { CalendarDays, ClipboardList, FileText, Plus, Printer } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { demo } from "../lib/acceso";
import { pesos } from "../lib/calculo";
import { ErrorDemo, type Detalle, type Pagina } from "../lib/tipos";
import { Aviso, Cargando } from "./elementos";

const fechaCorta = new Intl.DateTimeFormat("es-AR", {
  day: "2-digit", month: "short", year: "numeric", timeZone: "America/Argentina/Buenos_Aires",
});
const cantidad = new Intl.NumberFormat("es-AR", { maximumFractionDigits: 3 });

export function Historial() {
  const id = useSearchParams().get("presupuesto");
  return id ? <PresupuestoGuardado key={id} id={id} /> : <ListaPresupuestos />;
}

function ListaPresupuestos() {
  const [pagina, setPagina] = useState<Pagina>();
  const [error, setError] = useState(false);
  const [cargandoMas, setCargandoMas] = useState(false);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let vigente = true;
    void demo.historial().then((resultado) => {
      if (vigente) setPagina(resultado);
    }).catch(() => {
      if (vigente) setError(true);
    });
    return () => { vigente = false; };
  }, [intento]);

  async function cargarMas() {
    if (!pagina?.siguiente || cargandoMas) return;
    setCargandoMas(true);
    setError(false);
    try {
      const siguiente = await demo.historial(pagina.siguiente);
      setPagina({ presupuestos: [...pagina.presupuestos, ...siguiente.presupuestos], siguiente: siguiente.siguiente });
    } catch {
      setError(true);
    } finally {
      setCargandoMas(false);
    }
  }

  return (
    <div className="pagina">
      <header className="encabezado">
        <div><h1>Historial</h1><p>Consultá los presupuestos que guardaste.</p></div>
        <Link className="boton primario" href="/"><Plus size={18} aria-hidden="true" /> Nuevo presupuesto</Link>
      </header>
      {!pagina && !error && <Cargando>Cargando historial…</Cargando>}
      {!pagina && error && <Aviso>No pude abrir el historial. <button className="enlace" onClick={() => {
        setError(false);
        setIntento((valor) => valor + 1);
      }}>Volver a intentar</button></Aviso>}
      {pagina?.presupuestos.length === 0 && (
        <div className="tarjeta estado-vacio">
          <span className="icono-grande"><ClipboardList size={26} aria-hidden="true" /></span>
          <h2>Todavía no guardaste presupuestos</h2>
          <Link className="enlace" href="/">Armá el primero</Link>
        </div>
      )}
      {pagina && pagina.presupuestos.length > 0 && (
        <section className="tarjeta lista" aria-label="Presupuestos guardados">
          <table className="tabla">
            <caption className="solo-lector">Presupuestos guardados, del más reciente al más antiguo</caption>
            <thead><tr><th scope="col">Cliente</th><th scope="col">Fecha</th><th scope="col">Total</th><th scope="col"><span className="solo-lector">Detalle</span></th></tr></thead>
            <tbody>
              {pagina.presupuestos.map((presupuesto) => (
                <tr key={presupuesto.id}>
                  <th scope="row"><strong>{presupuesto.cliente.nombre}</strong><small>{presupuesto.cliente.localidad}</small></th>
                  <td><time dateTime={presupuesto.fecha}>{fechaCorta.format(new Date(presupuesto.fecha))}</time></td>
                  <td className="celda-importe">{pesos(presupuesto.total)}</td>
                  <td><Link className="enlace" href={`/historial/?presupuesto=${encodeURIComponent(presupuesto.id)}`}
                    aria-label={`Abrir presupuesto de ${presupuesto.cliente.nombre}`}>Abrir</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
          {pagina.siguiente && (
            <div className="tarjeta-pie">
              {error && <Aviso>No pude cargar más presupuestos.</Aviso>}
              <button className="boton secundario" type="button" disabled={cargandoMas} onClick={cargarMas}>
                {cargandoMas ? "Cargando…" : "Ver más presupuestos"}
              </button>
            </div>
          )}
        </section>
      )}
    </div>
  );
}

function PresupuestoGuardado({ id }: { id: string }) {
  const [presupuesto, setPresupuesto] = useState<Detalle>();
  const [error, setError] = useState("");
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let vigente = true;
    void demo.detalle(id).then((resultado) => {
      if (vigente) setPresupuesto(resultado);
    }).catch((problema: unknown) => {
      if (vigente) setError(problema instanceof ErrorDemo ? problema.message : "No pude abrir el presupuesto.");
    });
    return () => { vigente = false; };
  }, [id, intento]);

  return (
    <div className="pagina">
      <div className="miga"><Link className="enlace" href="/historial/">Volver al historial</Link></div>
      <header className="encabezado encabezado-detalle">
        <div><h1>Presupuesto guardado</h1><p>Instalaciones eléctricas</p></div>
        {presupuesto && <button className="boton secundario" type="button" onClick={() => window.print()}>
          <Printer size={18} aria-hidden="true" /> Imprimir o guardar PDF
        </button>}
      </header>
      {!presupuesto && !error && <Cargando>Abriendo el presupuesto…</Cargando>}
      {error && <Aviso>{error} <button className="enlace" onClick={() => {
        setError("");
        setIntento((valor) => valor + 1);
      }}>Volver a intentar</button></Aviso>}
      {presupuesto && (
        <article className="tarjeta comprobante" aria-label="Detalle del presupuesto">
          <div className="comprobante-cabecera">
            <div>
              <span className="etiqueta">Cliente</span>
              <h2>{presupuesto.cliente.nombre}</h2>
              <p>{presupuesto.cliente.localidad}</p>
            </div>
            <div className="comprobante-fecha">
              <CalendarDays size={18} aria-hidden="true" />
              <time dateTime={presupuesto.fecha}>{fechaCorta.format(new Date(presupuesto.fecha))}</time>
            </div>
          </div>
          <div className="tabla-contenedor">
            <table className="tabla">
              <caption className="solo-lector">Conceptos del presupuesto</caption>
              <thead><tr><th scope="col">Concepto</th><th scope="col">Cantidad</th><th scope="col">Importe</th></tr></thead>
              <tbody>
                {presupuesto.renglones.map((renglon, posicion) => (
                  <tr key={posicion}>
                    <th scope="row"><strong>{renglon.nombre}</strong><small>{pesos(renglon.precio)} / {renglon.unidad}</small></th>
                    <td>{cantidad.format(Number(renglon.cantidad))} {renglon.unidad}</td>
                    <td className="celda-importe">{pesos(renglon.importe)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="comprobante-pie">
            <span className="documento-demo"><FileText size={16} aria-hidden="true" /> Presupuesto de ejemplo</span>
            <dl className="totales">
              <div><dt>Subtotal</dt><dd>{pesos(presupuesto.subtotal)}</dd></div>
              <div><dt>Descuento ({cantidad.format(Number(presupuesto.descuentoPorcentaje))} %)</dt><dd>− {pesos(presupuesto.descuento)}</dd></div>
              <div className="total"><dt>Total</dt><dd data-testid="total-guardado">{pesos(presupuesto.total)}</dd></div>
            </dl>
          </div>
        </article>
      )}
    </div>
  );
}
