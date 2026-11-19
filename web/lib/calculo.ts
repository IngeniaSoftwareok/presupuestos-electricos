import type { Campos, Concepto, Datos, Pedido, Renglon, Totales } from "./tipos.ts";

export const MAX_RENGLONES = 20;

const formatoCantidad = /^[0-9]{1,4}(?:\.[0-9]{1,3})?$/;
const formatoDescuento = /^[0-9]{1,3}(?:\.[0-9]{1,2})?$/;
const formatoMoneda = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
});

function entero(decimal: string, escala: number): bigint {
  const [parteEntera, parteDecimal = ""] = decimal.split(".");
  return BigInt(parteEntera + parteDecimal.padEnd(escala, "0"));
}

function decimal(valor: bigint, escala: number): string {
  const texto = valor.toString().padStart(escala + 1, "0");
  return texto.slice(0, -escala) + "." + texto.slice(-escala);
}

function normalizar(valor: string): string {
  return valor.trim().replace(",", ".");
}

function cantidadValida(valor: string): boolean {
  return formatoCantidad.test(valor) && entero(valor, 3) > 0n;
}

function descuentoValido(valor: string): boolean {
  return formatoDescuento.test(valor) && entero(valor, 2) <= 10000n;
}

export function prepararPedido(datos: Datos, entrada: Pedido): { pedido: Pedido; campos: Campos } {
  const campos: Campos = {};
  if (!datos.clientes.some((cliente) => cliente.codigo === entrada.cliente)) {
    campos.cliente = "Elegí un cliente de la lista.";
  }

  const descuentoIngresado = normalizar(entrada.descuento) || "0";
  const descuento = descuentoValido(descuentoIngresado) ? decimal(entero(descuentoIngresado, 2), 2) : "0.00";
  if (!descuentoValido(descuentoIngresado)) {
    campos.descuento = "Ingresá un descuento entre 0 y 100, con hasta dos decimales.";
  }

  if (entrada.renglones.length < 1 || entrada.renglones.length > MAX_RENGLONES) {
    campos.renglones = "Agregá entre 1 y 20 conceptos.";
  }
  const renglones = entrada.renglones.map((renglon, posicion) => {
    if (!datos.conceptos.some((concepto) => concepto.codigo === renglon.concepto)) {
      campos[`renglones[${posicion}].concepto`] = "Elegí un concepto del catálogo.";
    }
    const cantidad = normalizar(renglon.cantidad);
    if (!cantidadValida(cantidad)) {
      campos[`renglones[${posicion}].cantidad`] = "Ingresá una cantidad mayor a cero, con hasta tres decimales.";
    }
    return { concepto: renglon.concepto, cantidad: cantidadValida(cantidad) ? decimal(entero(cantidad, 3), 3) : cantidad };
  });
  return { pedido: { cliente: entrada.cliente, descuento, renglones }, campos };
}

export function calcularRenglon(concepto: Concepto, cantidadIngresada: string): Renglon | null {
  const cantidad = normalizar(cantidadIngresada);
  if (!cantidadValida(cantidad)) return null;

  // Trabajo en centavos para que la vista previa redondee igual que el cálculo en Java.
  const milesimas = entero(cantidad, 3);
  const importe = (milesimas * entero(concepto.precio, 2) + 500n) / 1000n;
  return {
    concepto: concepto.codigo,
    nombre: concepto.nombre,
    unidad: concepto.unidad,
    precio: concepto.precio,
    cantidad: decimal(milesimas, 3),
    importe: decimal(importe, 2),
  };
}

export function calcularTotales(renglones: Renglon[], descuentoIngresado: string): Totales {
  const porcentaje = normalizar(descuentoIngresado);
  const subtotal = renglones.reduce((suma, renglon) => suma + entero(renglon.importe, 2), 0n);
  const descuento = descuentoValido(porcentaje) ? (subtotal * entero(porcentaje, 2) + 5000n) / 10000n : 0n;
  return { subtotal: decimal(subtotal, 2), descuento: decimal(descuento, 2), total: decimal(subtotal - descuento, 2) };
}

export function pesos(valor: string): string {
  return formatoMoneda.format(valor as `${number}`);
}
