import { FileText, Plus, Trash2 } from "lucide-react";
import { MAX_RENGLONES, pesos } from "../lib/calculo";
import type { Campos, Concepto, ItemPedido, Renglon } from "../lib/tipos";
import { ErrorCampo, Selector } from "./elementos";

export type Fila = ItemPedido & { fila: number };
export type FilaCalculada = { fila: Fila; concepto?: Concepto; calculo: Renglon | null };

type Propiedades = {
  filas: FilaCalculada[];
  catalogo: Concepto[];
  campos: Campos;
  bloqueado: boolean;
  campoId: (campo: string) => string;
  editar: (fila: number, cambio: Partial<ItemPedido>, campo: string) => void;
  agregar: () => void;
  quitar: (fila: number, posicion: number) => void;
};

export function Conceptos({ filas, catalogo, campos, bloqueado, campoId, editar, agregar, quitar }: Propiedades) {
  return (
    <section className="tarjeta" aria-labelledby="titulo-conceptos">
      <div className="tarjeta-cabecera">
        <span className="icono-seccion"><FileText size={19} aria-hidden="true" /></span>
        <h2 id="titulo-conceptos">Conceptos</h2>
      </div>
      <div className="grilla-titulos" aria-hidden="true">
        <span>Material o servicio</span><span>Cantidad</span><span>Importe</span><span></span>
      </div>
      <div className="grilla-renglones">
        {filas.map(({ fila, concepto, calculo }, posicion) => {
          const campoConcepto = `renglones[${posicion}].concepto`;
          const campoCantidad = `renglones[${posicion}].cantidad`;
          return (
            <div className="grilla-renglon" key={fila.fila} role="group" aria-label={`Concepto ${posicion + 1}`}>
              <div className="campo campo-concepto">
                <label className="solo-lector" htmlFor={campoId(campoConcepto)}>{`Material o servicio ${posicion + 1}`}</label>
                <Selector id={campoId(campoConcepto)} data-fila={fila.fila} value={fila.concepto} disabled={bloqueado}
                  aria-invalid={Boolean(campos[campoConcepto])}
                  aria-describedby={campos[campoConcepto] ? campoId(campoConcepto + "-error") : undefined}
                  onChange={(evento) => editar(fila.fila, { concepto: evento.target.value }, campoConcepto)}>
                  <option value="">Elegí un concepto</option>
                  {["Materiales", "Mano de obra"].map((rubro) => (
                    <optgroup key={rubro} label={rubro}>
                      {catalogo.filter((item) => item.rubro === rubro).map((item) =>
                        <option key={item.codigo} value={item.codigo}>{item.nombre}</option>)}
                    </optgroup>
                  ))}
                </Selector>
                {concepto && <small className="unitario">{pesos(concepto.precio)} / {concepto.unidad}</small>}
                <ErrorCampo id={campoId(campoConcepto + "-error")} mensaje={campos[campoConcepto]} />
              </div>
              <div className="campo campo-cantidad">
                <label className="solo-lector" htmlFor={campoId(campoCantidad)}>{`Cantidad ${posicion + 1}`}</label>
                <input id={campoId(campoCantidad)} inputMode="decimal" maxLength={8} value={fila.cantidad}
                  disabled={bloqueado} aria-invalid={Boolean(campos[campoCantidad])}
                  aria-describedby={campos[campoCantidad] ? campoId(campoCantidad + "-error") : undefined}
                  onChange={(evento) => editar(fila.fila, { cantidad: evento.target.value }, campoCantidad)} />
                <ErrorCampo id={campoId(campoCantidad + "-error")} mensaje={campos[campoCantidad]} />
              </div>
              <div className="importe-fila">
                <span className="solo-lector">Importe: </span>{calculo ? pesos(calculo.importe) : "—"}
              </div>
              <button className="boton-icono" type="button" aria-label={`Quitar concepto ${posicion + 1}`}
                onClick={() => quitar(fila.fila, posicion)} disabled={bloqueado || filas.length === 1}>
                <Trash2 size={17} aria-hidden="true" />
              </button>
            </div>
          );
        })}
      </div>
      <div className="tarjeta-pie">
        <button className="boton enlace" type="button" onClick={agregar} id={campoId("renglones")}
          disabled={bloqueado || filas.length >= MAX_RENGLONES}>
          <Plus size={18} aria-hidden="true" /> Agregar concepto
        </button>
        <ErrorCampo id={campoId("renglones-error")} mensaje={campos.renglones} />
      </div>
    </section>
  );
}
