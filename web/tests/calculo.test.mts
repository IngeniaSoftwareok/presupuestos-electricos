import assert from "node:assert/strict";
import test from "node:test";
import { calcularRenglon, calcularTotales, prepararPedido } from "../lib/calculo.ts";
import type { Concepto, Datos } from "../lib/tipos.ts";

const cable: Concepto = { codigo: "cable", nombre: "Cable", unidad: "m", rubro: "Materiales", precio: "1005.00" };
const datos: Datos = { clientes: [{ codigo: "casa", nombre: "Casa", localidad: "Haedo" }], conceptos: [cable] };

test("redondea cada renglón antes de sumar", () => {
  const renglon = calcularRenglon(cable, "0,001")!;
  assert.equal(renglon.importe, "1.01");
  assert.deepEqual(calcularTotales([renglon, renglon], "0"), { subtotal: "2.02", descuento: "0.00", total: "2.02" });
});

test("aplica el descuento sobre el subtotal y redondea los medios centavos", () => {
  const renglon = calcularRenglon({ ...cable, precio: "0.05" }, "1")!;
  assert.deepEqual(calcularTotales([renglon], "10"), { subtotal: "0.05", descuento: "0.01", total: "0.04" });
  assert.equal(calcularTotales([renglon], "100").total, "0.00");
});

test("unifica separadores y escala antes de enviar", () => {
  const resultado = prepararPedido(datos, { cliente: "casa", descuento: "2,5", renglones: [{ concepto: "cable", cantidad: "1.7" }] });
  assert.deepEqual(resultado, { pedido: {
    cliente: "casa", descuento: "2.50", renglones: [{ concepto: "cable", cantidad: "1.700" }],
  }, campos: {} });
});

test("marca descuentos, cantidades y conceptos inválidos", () => {
  const resultado = prepararPedido(datos, { cliente: "casa", descuento: "101", renglones: [{ concepto: "otro", cantidad: "0" }] });
  assert.deepEqual(Object.keys(resultado.campos).sort(), ["descuento", "renglones[0].cantidad", "renglones[0].concepto"]);
  for (const valor of ["0", "-1", "1,0001", "1e2", "10000"]) assert.equal(calcularRenglon(cable, valor), null);
});
