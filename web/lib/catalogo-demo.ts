import type { Datos } from "./tipos";

export const catalogoDemo: Datos = {
  clientes: [
    { codigo: "casa-laprida", nombre: "Casa Laprida", localidad: "Villa Ballester" },
    { codigo: "estudio-arce", nombre: "Estudio Arce", localidad: "Haedo" },
    { codigo: "local-norte", nombre: "Local Norte", localidad: "Olivos" },
  ],
  conceptos: [
    { codigo: "mano-obra", nombre: "Instalación eléctrica", unidad: "h", rubro: "Mano de obra", precio: "18500.00" },
    { codigo: "revision", nombre: "Revisión de tablero", unidad: "u", rubro: "Mano de obra", precio: "26500.00" },
    { codigo: "cable-25", nombre: "Cable unipolar 2,5 mm²", unidad: "m", rubro: "Materiales", precio: "1450.00" },
    { codigo: "canaleta", nombre: "Canaleta 20 × 10 mm", unidad: "m", rubro: "Materiales", precio: "2350.00" },
    { codigo: "disyuntor", nombre: "Disyuntor diferencial 40 A", unidad: "u", rubro: "Materiales", precio: "68500.00" },
    { codigo: "termica", nombre: "Llave térmica bipolar 20 A", unidad: "u", rubro: "Materiales", precio: "29800.00" },
    { codigo: "toma-doble", nombre: "Tomacorriente doble", unidad: "u", rubro: "Materiales", precio: "9200.00" },
  ],
};
