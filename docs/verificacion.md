# API y verificación

La web y la API se sirven en el mismo origen. Primero se llama a `GET /api/datos`, que devuelve los clientes y el catálogo e inicia el espacio del navegador. Después se puede crear y volver a leer un presupuesto.

| Método y ruta | Respuesta |
| --- | --- |
| `GET /api/datos` | Catálogo y clientes ficticios. |
| `POST /api/presupuestos` | Presupuesto guardado. Devuelve `201` para un alta y `200` para la repetición del mismo pedido. |
| `GET /api/presupuestos` | Historial paginado. Enviá `?antesDe=` con el cursor devuelto para continuar. |
| `GET /api/presupuestos/{id}` | Detalle guardado con cantidades e importes como cadenas decimales. |

El `POST` recibe `application/json` y una cabecera `Idempotency-Key` con un UUID. Si probás la API con otra herramienta, conservá la cookie que devuelve `/api/datos` y enviá el origen de la demo en la cabecera `Origin`.

```json
{
  "cliente": "casa-laprida",
  "descuento": "5.00",
  "renglones": [
    { "concepto": "cable-25", "cantidad": "18.500" },
    { "concepto": "termica", "cantidad": "2.000" },
    { "concepto": "mano-obra", "cantidad": "3.000" }
  ]
}
```

## Pruebas

La integración de Java usa MySQL 8.4 y el servidor HTTP real. Levantá una instancia local con el override de pruebas desde la raíz.

```sh
docker compose -f compose.yaml -f compose.pruebas.yaml up -d --wait db
```

En `api/`, con Java 21 y Maven, configurá `DEMO_DB_URL` con el valor indicado y ejecutá `mvn verify`. Los ejemplos de la suite usan la contraseña de desarrollo de Compose.

```text
jdbc:mysql://localhost:33316/presupuestos?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true
```

En `web/`, con Node 24, corré `npm ci` y `npm test` para las cuentas y validaciones de la interfaz. Con el proyecto completo en `http://localhost:8083`, ejecutá `npx playwright install chromium` y `npm run test:e2e`. La [configuración de GitHub Actions](../.github/workflows/verificar.yml) incluye las pruebas de ambas ediciones.
