import AxeBuilder from "@axe-core/playwright";
import { expect, test } from "@playwright/test";

const estimado = /134\.828,75/;
const modoNavegador = process.env.DEMO_PRUEBA_NAVEGADOR === "1";
const inyeccionDelHosting = "(function(){function c(){var b=a.content";

test("crea, reabre e imprime el presupuesto con los mismos importes", async ({ page }) => {
  type Infraccion = { directiva: string; bloqueado: string; muestra: string };
  const infracciones: Infraccion[] = [];
  await page.exposeFunction("registrarCsp", (detalle: Infraccion) => infracciones.push(detalle));
  await page.addInitScript(() => {
    document.addEventListener("securitypolicyviolation", (evento) => {
      void (window as typeof window & { registrarCsp: (detalle: Infraccion) => void })
        .registrarCsp({ directiva: evento.violatedDirective, bloqueado: evento.blockedURI, muestra: evento.sample });
    });
    window.print = () => document.documentElement.setAttribute("data-impresion", "solicitada");
  });

  await page.goto("./");
  await page.getByRole("button", { name: "Guardar presupuesto" }).click();
  await expect(page.getByRole("region", { name: "Resumen" }).getByRole("alert")).toContainText("Revisá los campos");
  await expect(page.getByLabel("Cliente de ejemplo")).toBeFocused();

  await page.getByRole("button", { name: "Cargar ejemplo" }).click();
  await expect(page.getByTestId("total-estimado")).toHaveText(estimado);
  await page.getByRole("button", { name: "Agregar concepto" }).click();
  await expect(page.getByLabel("Material o servicio 4")).toBeFocused();
  await page.getByLabel("Material o servicio 4").selectOption("revision");
  await expect(page.getByTestId("total-estimado")).toHaveText(/160\.003,75/);
  await page.getByRole("button", { name: "Quitar concepto 4" }).click();
  await expect(page.getByTestId("total-estimado")).toHaveText(estimado);

  await page.getByLabel("Cantidad 1", { exact: true }).fill("0");
  await page.getByRole("button", { name: "Guardar presupuesto" }).click();
  await expect(page.getByLabel("Cantidad 1", { exact: true })).toHaveAttribute("aria-invalid", "true");
  await page.getByLabel("Cantidad 1", { exact: true }).fill("18,5");

  await page.getByRole("button", { name: "Guardar presupuesto" }).click();
  await expect(page.getByRole("heading", { name: "Presupuesto guardado" })).toBeVisible();
  await expect(page.getByTestId("total-guardado")).toHaveText(estimado);
  await page.getByRole("button", { name: "Imprimir o guardar PDF" }).click();
  await expect(page.locator("html")).toHaveAttribute("data-impresion", "solicitada");
  await page.reload();
  await expect(page.getByTestId("total-guardado")).toHaveText(estimado);
  await page.getByRole("link", { name: "Volver al historial" }).click();
  await expect(page.getByRole("row")).toHaveCount(2);
  await page.getByRole("link", { name: "Abrir presupuesto de Casa Laprida" }).click();
  await expect(page.getByTestId("total-guardado")).toHaveText(estimado);
  await page.getByRole("navigation", { name: "Principal" }).getByRole("link", { name: "Nuevo presupuesto" }).click();
  await expect(page.getByRole("button", { name: "Guardar presupuesto" })).toBeEnabled();
  await expect(page.getByLabel("Cliente de ejemplo")).toHaveValue("");
  // En Sites, Cloudflare inyecta un bloque con valores distintos por respuesta. La demo funciona con ese bloque deshabilitado.
  const inesperadas = infracciones.filter(({ directiva, bloqueado, muestra }) => !(
    process.env.DEMO_PRUEBA_SITES === "1" && directiva === "script-src-elem"
    && bloqueado === "inline" && muestra.startsWith(inyeccionDelHosting)
  ));
  expect(inesperadas).toEqual([]);
});

test("se puede operar desde un teléfono sin desbordar la página", async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 800 });
  await page.goto("./");
  await page.getByRole("button", { name: "Cargar ejemplo" }).click();
  const sinDesborde = () => page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth);
  expect(await sinDesborde()).toBeTruthy();
  await page.getByRole("button", { name: "Guardar presupuesto" }).click();
  await expect(page.getByTestId("total-guardado")).toHaveText(estimado);
  expect(await sinDesborde()).toBeTruthy();
  await page.getByRole("link", { name: "Volver al historial" }).click();
  await expect(page.getByRole("link", { name: "Abrir presupuesto de Casa Laprida" })).toBeVisible();
  expect(await sinDesborde()).toBeTruthy();
});

test("confirma la misma operación si el guardado entró pero se perdió la respuesta", async ({ page }) => {
  test.skip(modoNavegador, "Esta prueba necesita la API Spring Boot.");
  let clave = "";
  let id = "";
  await page.route("**/api/presupuestos", async (ruta) => {
    if (ruta.request().method() !== "POST") return ruta.continue();
    const respuesta = await ruta.fetch();
    clave = ruta.request().headers()["idempotency-key"];
    expect(respuesta.status()).toBe(201);
    id = (await respuesta.json()).id;
    await ruta.abort("failed");
  }, { times: 1 });

  await page.goto("./");
  await page.getByRole("button", { name: "Cargar ejemplo" }).click();
  await page.getByRole("button", { name: "Guardar presupuesto" }).click();
  const aviso = page.getByRole("region", { name: "Resumen" }).getByRole("alert");
  await expect(aviso).toContainText("No pude confirmar");
  await page.reload();
  await expect(aviso).toContainText("guardado pendiente");
  const siguiente = page.waitForResponse((respuesta) => respuesta.request().method() === "POST");
  await page.getByRole("button", { name: "Reintentar guardado" }).click();
  const respuesta = await siguiente;
  expect(respuesta.request().headers()["idempotency-key"]).toBe(clave);
  expect(respuesta.status()).toBe(200);
  await expect(page).toHaveURL(new RegExp(id));
  await page.getByRole("link", { name: "Volver al historial" }).click();
  await expect(page.getByRole("row")).toHaveCount(2);
});

test("el editor y el presupuesto guardado pasan el control automático de accesibilidad", async ({ page }) => {
  const auditar = async () => {
    const informe = await new AxeBuilder({ page })
      .withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"])
      .analyze();
    expect(informe.violations.map(({ id, nodes }) => ({ id, elementos: nodes.map(({ target }) => target) }))).toEqual([]);
  };
  await page.goto("./");
  await page.getByRole("button", { name: "Cargar ejemplo" }).click();
  await auditar();
  await page.getByRole("button", { name: "Guardar presupuesto" }).click();
  await expect(page.getByTestId("total-guardado")).toHaveText(estimado);
  await auditar();
});

test("la edición web guarda una sola copia si se reintenta desde dos pestañas", async ({ context, page }) => {
  test.skip(!modoNavegador, "El backend tiene su propia prueba de concurrencia con MySQL.");
  const intento = {
    clave: crypto.randomUUID(),
    pedido: { cliente: "casa-laprida", descuento: "0.00", renglones: [{ concepto: "cable-25", cantidad: "1.000" }] },
  };
  const otra = await context.newPage();
  for (const pestaña of [page, otra]) {
    await pestaña.addInitScript((valor) => {
      if (location.pathname.endsWith("/historial/")) return;
      sessionStorage.setItem("presupuestos-electricos:guardado-pendiente", JSON.stringify(valor));
    }, intento);
  }
  await Promise.all([page.goto("./"), otra.goto("./")]);
  await Promise.all([page, otra].map((pestaña) =>
    pestaña.getByRole("button", { name: "Reintentar guardado" }).click()));
  await Promise.all([page, otra].map((pestaña) =>
    expect(pestaña.getByTestId("total-guardado")).toHaveText(/1\.450,00/)));
  expect(page.url()).toBe(otra.url());
  await page.getByRole("link", { name: "Volver al historial" }).click();
  await expect(page.getByRole("row")).toHaveCount(2);
});
