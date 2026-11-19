"use client";

import { ClipboardCopy, ReceiptText, Save, UserRound } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useId, useRef, useState, type FormEvent } from "react";
import { demo } from "../lib/acceso";
import { calcularRenglon, calcularTotales, pesos, prepararPedido } from "../lib/calculo";
import { ErrorDemo, type Campos, type Datos, type ItemPedido, type Pedido } from "../lib/tipos";
import { Conceptos, type Fila } from "./conceptos";
import { Aviso, Cargando, ErrorCampo, Selector } from "./elementos";

type Intento = { clave: string; pedido: Pedido };
const claveIntento = "presupuestos-electricos:guardado-pendiente";
const ejemplo: Pedido = {
  cliente: "casa-laprida",
  descuento: "5",
  renglones: [
    { concepto: "cable-25", cantidad: "18,5" },
    { concepto: "termica", cantidad: "2" },
    { concepto: "mano-obra", cantidad: "3" },
  ],
};

function leerIntento(): Intento | null {
  try {
    const valor = sessionStorage.getItem(claveIntento);
    return valor ? JSON.parse(valor) as Intento : null;
  } catch {
    return null;
  }
}

function recordarIntento(intento: Intento | null) {
  try {
    if (intento) sessionStorage.setItem(claveIntento, JSON.stringify(intento));
    else sessionStorage.removeItem(claveIntento);
  } catch {
    // Con el almacenamiento bloqueado, el estado del editor conserva el intento mientras siga abierto.
  }
}

export function Editor() {
  const router = useRouter();
  const prefijo = useId();
  const ultimaFila = useRef(1);
  const enviando = useRef(false);
  const [datos, setDatos] = useState<Datos>();
  const [errorCarga, setErrorCarga] = useState(false);
  const [cliente, setCliente] = useState("");
  const [descuento, setDescuento] = useState("");
  const [filas, setFilas] = useState<Fila[]>([{ fila: 1, concepto: "", cantidad: "1" }]);
  const [campos, setCampos] = useState<Campos>({});
  const [aviso, setAviso] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const [pendiente, setPendiente] = useState<Intento | null>(null);

  function cargarDatos() {
    setErrorCarga(false);
    void demo.datos().then(setDatos).catch(() => setErrorCarga(true));
  }

  function cargarPedido(pedido: Pedido) {
    setCliente(pedido.cliente);
    setDescuento(pedido.descuento.replace(".", ","));
    setFilas(pedido.renglones.map((renglon) => ({
      fila: ++ultimaFila.current, concepto: renglon.concepto, cantidad: renglon.cantidad.replace(".", ","),
    })));
    setCampos({});
    setAviso("");
  }

  useEffect(() => {
    cargarDatos();
    const intento = leerIntento();
    if (intento) {
      cargarPedido(intento.pedido);
      setPendiente(intento);
      setAviso("Quedó un guardado pendiente. Reintentá para confirmar qué pasó.");
    }
  }, []);

  const seleccion = datos?.clientes.find((item) => item.codigo === cliente);
  const calculadas = filas.map((fila) => {
    const concepto = datos?.conceptos.find((item) => item.codigo === fila.concepto);
    return { fila, concepto, calculo: concepto ? calcularRenglon(concepto, fila.cantidad) : null };
  });
  const renglones = calculadas.flatMap(({ calculo }) => calculo ? [calculo] : []);
  const totales = calcularTotales(renglones, descuento);
  const bloqueado = ocupado || pendiente !== null;
  const campoId = (campo: string) => `${prefijo}-${campo}`;

  function corregir(campo: string) {
    setCampos((anteriores) => {
      const restantes = { ...anteriores };
      delete restantes[campo];
      return restantes;
    });
  }

  function editarFila(fila: number, cambio: Partial<ItemPedido>, campo: string) {
    setFilas((anteriores) => anteriores.map((item) => item.fila === fila ? { ...item, ...cambio } : item));
    corregir(campo);
  }

  function agregar() {
    const fila = ++ultimaFila.current;
    setFilas((anteriores) => [...anteriores, { fila, concepto: "", cantidad: "1" }]);
    corregir("renglones");
    requestAnimationFrame(() => document.querySelector<HTMLElement>(`[data-fila="${fila}"]`)?.focus());
  }

  function quitar(fila: number, posicion: number) {
    setFilas((anteriores) => anteriores.filter((item) => item.fila !== fila));
    setCampos((anteriores) => {
      const actualizados: Campos = {};
      for (const [campo, mensaje] of Object.entries(anteriores)) {
        const item = /^renglones\[(\d+)\](.*)$/.exec(campo);
        if (!item || Number(item[1]) < posicion) actualizados[campo] = mensaje;
        else if (Number(item[1]) > posicion) actualizados[`renglones[${Number(item[1]) - 1}]${item[2]}`] = mensaje;
      }
      return actualizados;
    });
  }

  async function guardar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (!datos || enviando.current) return;

    const { pedido, campos: errores } = prepararPedido(datos, { cliente, descuento, renglones: filas });
    if (!pendiente && Object.keys(errores).length > 0) {
      setCampos(errores);
      setAviso("Revisá los campos marcados.");
      const primero = Object.keys(errores)[0];
      requestAnimationFrame(() => document.getElementById(campoId(primero))?.focus());
      return;
    }

    // Si se corta la conexión, el siguiente envío conserva la misma clave y los mismos datos.
    const intento = pendiente ?? { clave: crypto.randomUUID(), pedido };
    enviando.current = true;
    setOcupado(true);
    setAviso("");
    recordarIntento(intento);
    try {
      const presupuesto = await demo.guardar(intento.pedido, intento.clave);
      recordarIntento(null);
      router.push(`/historial/?presupuesto=${encodeURIComponent(presupuesto.id)}`);
    } catch (error) {
      enviando.current = false;
      setOcupado(false);
      if (error instanceof ErrorDemo && error.estado < 500) {
        recordarIntento(null);
        setPendiente(null);
        setCampos(error.campos);
        setAviso(error.message);
      } else {
        setPendiente(intento);
        setAviso("No pude confirmar el guardado. Reintentá con el mismo presupuesto.");
      }
    }
  }

  return (
    <div className="pagina">
      <header className="encabezado">
        <div><h1>Nuevo presupuesto</h1><p>Elegí un cliente y agregá los materiales y la mano de obra.</p></div>
        <button className="boton secundario" type="button" onClick={() => cargarPedido(ejemplo)} disabled={!datos || bloqueado}>
          <ClipboardCopy size={18} aria-hidden="true" /> Cargar ejemplo
        </button>
      </header>
      {errorCarga && <Aviso>No pude cargar el catálogo. <button className="enlace" onClick={cargarDatos}>Volver a intentar</button></Aviso>}
      {!datos && !errorCarga && <Cargando>Cargando el catálogo…</Cargando>}
      {datos && (
        <form className="editor" onSubmit={guardar} noValidate aria-busy={ocupado}>
          <div className="editor-campos">
            <section className="tarjeta" aria-labelledby="titulo-cliente">
              <div className="tarjeta-cabecera">
                <span className="icono-seccion"><UserRound size={19} aria-hidden="true" /></span>
                <h2 id="titulo-cliente">Cliente</h2>
              </div>
              <div className="tarjeta-cuerpo dos-columnas">
                <div className="campo">
                  <label htmlFor={campoId("cliente")}>Cliente de ejemplo</label>
                  <Selector id={campoId("cliente")} value={cliente} disabled={bloqueado} aria-invalid={Boolean(campos.cliente)}
                    aria-describedby={campos.cliente ? campoId("cliente-error") : undefined}
                    onChange={(evento) => { setCliente(evento.target.value); corregir("cliente"); }}>
                    <option value="">Elegí un cliente</option>
                    {datos.clientes.map((item) => <option key={item.codigo} value={item.codigo}>{item.nombre}</option>)}
                  </Selector>
                  <ErrorCampo id={campoId("cliente-error")} mensaje={campos.cliente} />
                </div>
                <div className="campo">
                  <span className="etiqueta">Localidad</span>
                  <div className="solo-lectura">{seleccion?.localidad ?? "—"}</div>
                </div>
              </div>
            </section>
            <Conceptos filas={calculadas} catalogo={datos.conceptos} campos={campos} bloqueado={bloqueado}
              campoId={campoId} editar={editarFila} agregar={agregar} quitar={quitar} />
          </div>
          <section className="tarjeta resumen" aria-labelledby="titulo-resumen">
            <div className="tarjeta-cabecera">
              <span className="icono-seccion"><ReceiptText size={19} aria-hidden="true" /></span>
              <h2 id="titulo-resumen">Resumen</h2>
            </div>
            <div className="tarjeta-cuerpo">
              <div className="campo">
                <label htmlFor={campoId("descuento")}>Descuento</label>
                <div className="entrada-porcentaje">
                  <input id={campoId("descuento")} value={descuento} inputMode="decimal" maxLength={6}
                    placeholder="0" disabled={bloqueado} aria-invalid={Boolean(campos.descuento)}
                    aria-describedby={campos.descuento ? campoId("descuento-error") : undefined}
                    onChange={(evento) => { setDescuento(evento.target.value); corregir("descuento"); }} />
                  <span aria-hidden="true">%</span>
                </div>
                <ErrorCampo id={campoId("descuento-error")} mensaje={campos.descuento} />
              </div>
              <dl className="totales">
                <div><dt>Subtotal</dt><dd>{pesos(totales.subtotal)}</dd></div>
                <div><dt>Descuento</dt><dd>{totales.descuento === "0.00" ? "—" : `− ${pesos(totales.descuento)}`}</dd></div>
                <div className="total"><dt>Total estimado</dt><dd data-testid="total-estimado">{pesos(totales.total)}</dd></div>
              </dl>
              {aviso && <Aviso>{aviso}{pendiente && <> <Link href="/historial/">Ver historial</Link></>}</Aviso>}
              <button className="boton primario guardar" type="submit" disabled={ocupado}>
                <Save size={18} aria-hidden="true" />
                {ocupado ? "Guardando…" : pendiente ? "Reintentar guardado" : "Guardar presupuesto"}
              </button>
            </div>
          </section>
        </form>
      )}
    </div>
  );
}
