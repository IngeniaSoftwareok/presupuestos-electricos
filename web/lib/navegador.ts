import { calcularRenglon, calcularTotales, prepararPedido } from "./calculo";
import { catalogoDemo } from "./catalogo-demo";
import {
  ErrorDemo, type AccesoPresupuestos, type Detalle, type Pagina, type Pedido, type Renglon,
} from "./tipos";

type Registro = { secuencia?: number; clave: string; huella: string; presupuesto: Detalle };
const nombreBase = "ingenia-presupuestos-electricos";
const almacen = "presupuestos";
const limite = 15;
let apertura: Promise<IDBDatabase> | undefined;

function abrirBase(): Promise<IDBDatabase> {
  apertura ??= new Promise<IDBDatabase>((resolver, rechazar) => {
    const solicitud = indexedDB.open(nombreBase, 1);
    solicitud.onupgradeneeded = () => {
      const tabla = solicitud.result.createObjectStore(almacen, { keyPath: "secuencia", autoIncrement: true });
      tabla.createIndex("clave", "clave", { unique: true });
      tabla.createIndex("id", "presupuesto.id", { unique: true });
    };
    solicitud.onerror = () => rechazar(solicitud.error);
    solicitud.onblocked = () => rechazar(new ErrorDemo("Cerrá las otras pestañas de la demo y volvé a intentar.", 503));
    solicitud.onsuccess = () => {
      solicitud.result.onversionchange = () => {
        solicitud.result.close();
        apertura = undefined;
      };
      resolver(solicitud.result);
    };
  }).catch((error: unknown) => {
    apertura = undefined;
    throw error;
  });
  return apertura;
}

async function enBase<T>(
  modo: IDBTransactionMode,
  operar: (tabla: IDBObjectStore, listo: (valor: T) => void, cancelar: (error: ErrorDemo) => void) => void,
): Promise<T> {
  const base = await abrirBase();
  return new Promise<T>((resolver, rechazar) => {
    const transaccion = base.transaction(almacen, modo);
    let respuesta: T;
    let errorPropio: ErrorDemo | undefined;
    transaccion.oncomplete = () => resolver(respuesta);
    transaccion.onabort = () => rechazar(errorPropio ?? transaccion.error);
    operar(transaccion.objectStore(almacen), (valor) => { respuesta = valor; }, (error) => {
      errorPropio = error;
      transaccion.abort();
    });
  });
}

function crearDetalle(pedido: Pedido): Detalle {
  const cliente = catalogoDemo.clientes.find((item) => item.codigo === pedido.cliente)!;
  const renglones = pedido.renglones.map((renglon) => {
    const concepto = catalogoDemo.conceptos.find((item) => item.codigo === renglon.concepto)!;
    return calcularRenglon(concepto, renglon.cantidad)!;
  }) satisfies Renglon[];
  return { id: crypto.randomUUID(), cliente, fecha: new Date().toISOString(),
    descuentoPorcentaje: pedido.descuento, renglones, ...calcularTotales(renglones, pedido.descuento) };
}

export const navegador: AccesoPresupuestos = {
  async datos() {
    await abrirBase();
    return catalogoDemo;
  },
  async guardar(entrada, clave) {
    const { pedido, campos } = prepararPedido(catalogoDemo, entrada);
    if (Object.keys(campos).length > 0) throw new ErrorDemo("Revisá los datos del presupuesto.", 400, campos);
    const huella = JSON.stringify(pedido);
    const candidato = crearDetalle(pedido);

    // Hago la búsqueda y el alta en la misma transacción, incluso si la demo está abierta en dos pestañas.
    return enBase("readwrite", (tabla, listo, cancelar) => {
      const buscar = tabla.index("clave").get(clave);
      buscar.onsuccess = () => {
        const anterior: Registro | undefined = buscar.result;
        if (anterior && anterior.huella !== huella) {
          cancelar(new ErrorDemo("Este intento corresponde a otro presupuesto. Revisá el historial.", 409));
          return;
        }
        if (anterior) {
          listo(anterior.presupuesto);
          return;
        }
        tabla.add({ clave, huella, presupuesto: candidato } satisfies Registro);
        listo(candidato);
      };
    });
  },
  historial(antesDe) {
    return enBase<Pagina>("readonly", (tabla, listo) => {
      const rango = antesDe ? IDBKeyRange.upperBound(Number(antesDe), true) : null;
      const buscar = tabla.openCursor(rango, "prev");
      const registros: Registro[] = [];
      buscar.onsuccess = () => {
        const cursor = buscar.result;
        if (cursor && registros.length < limite + 1) {
          registros.push(cursor.value as Registro);
          cursor.continue();
          return;
        }
        const presupuestos = registros.slice(0, limite).map(({ presupuesto: { id, cliente, fecha, total } }) =>
          ({ id, cliente, fecha, total }));
        const siguiente = registros.length > limite ? String(registros[limite - 1].secuencia) : null;
        listo({ presupuestos, siguiente });
      };
    });
  },
  detalle(id) {
    return enBase<Detalle>("readonly", (tabla, listo, cancelar) => {
      const buscar = tabla.index("id").get(id);
      buscar.onsuccess = () => {
        const registro: Registro | undefined = buscar.result;
        if (registro) listo(registro.presupuesto);
        else cancelar(new ErrorDemo("Ese presupuesto no está en este historial.", 404));
      };
    });
  },
};
