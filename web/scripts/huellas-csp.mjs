import { createHash } from "node:crypto";
import { readFile, readdir, writeFile } from "node:fs/promises";
import { join } from "node:path";

const raiz = new URL("../out/", import.meta.url);
const archivos = await readdir(raiz, { recursive: true });
const huellas = new Set();
const insertarMeta = process.argv.includes("--meta");

// Next genera algunos scripts dentro del HTML. Autorizo esos bloques por su contenido.
for (const archivo of archivos.filter((ruta) => ruta.endsWith(".html"))) {
  const ruta = new URL(archivo.replaceAll("\\", "/"), raiz);
  const html = await readFile(ruta, "utf8");
  const deLaPagina = [];
  for (const [, atributos, bloque] of html.matchAll(/<script\b([^>]*)>([\s\S]*?)<\/script>/gi)) {
    if (!/\bsrc\s*=/.test(atributos) && bloque) {
      const huella = `'sha256-${createHash("sha256").update(bloque).digest("base64")}'`;
      huellas.add(huella);
      deLaPagina.push(huella);
    }
  }
  if (insertarMeta) {
    const meta = `<meta http-equiv="Content-Security-Policy" content="${politica(deLaPagina, false)}">`;
    await writeFile(ruta, html.replace("<head>", `<head>${meta}`));
  }
}

function politica(permisos, paraCabecera) {
  const reglas = [
    "default-src 'self'",
    ["script-src 'self' 'report-sample'", ...new Set(permisos)].join(" "),
    "script-src-attr 'none'",
    "style-src 'self'",
    "style-src-attr 'unsafe-inline'",
    "img-src 'self' data:",
    "connect-src 'self'",
    "object-src 'none'",
    "base-uri 'none'",
    "form-action 'self'",
  ];
  if (paraCabecera) reglas.push("frame-ancestors 'none'");
  return reglas.join("; ");
}

if (!insertarMeta) {
  const cabecera = politica([...huellas].sort(), true);
  await writeFile(join("scripts", "csp.conf"), `add_header Content-Security-Policy "${cabecera}" always;\n`);
}
