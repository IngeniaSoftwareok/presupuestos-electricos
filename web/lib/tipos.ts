export type Cliente = { codigo: string; nombre: string; localidad: string };
export type Concepto = { codigo: string; nombre: string; unidad: string; rubro: string; precio: string };
export type Datos = { clientes: Cliente[]; conceptos: Concepto[] };
export type ItemPedido = { concepto: string; cantidad: string };
export type Pedido = { cliente: string; descuento: string; renglones: ItemPedido[] };
export type Renglon = ItemPedido & {
  nombre: string;
  unidad: string;
  precio: string;
  importe: string;
};
export type Totales = { subtotal: string; descuento: string; total: string };
export type Resumen = { id: string; cliente: Cliente; fecha: string; total: string };
export type Detalle = Resumen & Totales & { descuentoPorcentaje: string; renglones: Renglon[] };
export type Pagina = { presupuestos: Resumen[]; siguiente: string | null };
export type Campos = Record<string, string>;

export interface AccesoPresupuestos {
  datos(): Promise<Datos>;
  guardar(pedido: Pedido, clave: string): Promise<Detalle>;
  historial(antesDe?: string): Promise<Pagina>;
  detalle(id: string): Promise<Detalle>;
}

export class ErrorDemo extends Error {
  constructor(
    mensaje: string,
    readonly estado: number,
    readonly campos: Campos = {},
  ) {
    super(mensaje);
  }
}
