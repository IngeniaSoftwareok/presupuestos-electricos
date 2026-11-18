# Demo de presupuestos en Java

Desarrollé esta demo independiente para el portfolio, con asistencia de IA. Calcula presupuestos a partir de un CSV y genera un resultado en JSON.

[![Pruebas Java](https://github.com/IngeniaSoftwareok/demo-presupuestos-java/actions/workflows/verify.yml/badge.svg)](https://github.com/IngeniaSoftwareok/demo-presupuestos-java/actions/workflows/verify.yml)

## Qué hace

El programa lee los conceptos de un CSV, calcula el subtotal y aplica un descuento y un recargo. Se ejecuta desde la terminal y escribe el resultado en JSON.

## Ejecutar

Requisitos: **JDK 21** y **Maven 3.9 o posterior**. La primera compilación puede descargar dependencias y plugins de Maven.

```sh
mvn --batch-mode --no-transfer-progress clean verify
java -jar target/demo-presupuestos-java-1.0.0.jar examples/presupuesto-demo.csv --descuento=10 --recargo=5
```

Resultado de referencia: subtotal **4351.50**, descuento **435.15**, recargo **195.82**, total **4112.17**. [Salida completa esperada](examples/resultado-esperado.json).

Sin argumentos o con `--help`, el programa muestra el uso. Entrada válida: código de salida 0. Error de entrada: código 2, mensaje por stderr y ningún resultado parcial por stdout.

## Entrada y reglas

- UTF-8, separador `;`, punto decimal, sin separador de miles. [CSV de ejemplo](examples/presupuesto-demo.csv).
- Cabecera exacta: `codigo;descripcion;cantidad;precio_unitario`.
- Se admiten campos entre comillas, separadores dentro de ellos y comillas duplicadas. No se admiten campos de varias líneas.
- Entre 1 y 200 conceptos. El comando admite un archivo de hasta 256 KiB.
- Cantidad positiva, hasta tres decimales; precio no negativo, hasta dos. No se truncan entradas inválidas.
- Cantidades y precios: hasta 16 dígitos significativos. Descuento y recargo entre 0 y 100, hasta dos decimales.
- Cada importe de línea se redondea a dos decimales con `HALF_UP`. El subtotal suma esas líneas. El descuento se calcula sobre el subtotal; el recargo, sobre el importe después del descuento. Cada ajuste también se redondea a dos decimales.
- Los importes se escriben como cadenas decimales en JSON para evitar conversiones implícitas a coma flotante.

El descuento y el recargo son porcentajes configurables sobre el presupuesto. El ejemplo utiliza una regla simple de cálculo, sin impuestos.

## Estructura

```text
src/main/java/ar/com/ingenia/quote/
  domain/    Entradas inmutables, reglas y resultado
  io/        Lectura CSV y escritura JSON
  cli/       Argumentos, archivos y códigos de salida
src/test/    Casos de negocio y entrada/salida
examples/    Datos ficticios y resultado reproducible
```

Separé las reglas de cálculo de la lectura CSV, la escritura JSON y los argumentos del comando. El JAR se ejecuta con el JDK; JUnit se utiliza para las pruebas.

## Pruebas

Incluí 15 pruebas de cálculo y entrada/salida: redondeo por línea, orden de los ajustes, descuento total, copia de los datos de entrada, lectura de CSV, escape de texto y rechazo de entradas inválidas.

GitHub Actions compila el proyecto, ejecuta las pruebas y corre el ejemplo en Java 21. El badge del inicio enlaza al workflow.

## Decisiones

Elegí `BigDecimal` para controlar la precisión y el redondeo de los importes. Mantuve las reglas en el dominio y la entrada/salida en adaptadores separados para poder probar el cálculo de forma independiente.

[Agustin Crudo · Ingenia Software](https://github.com/IngeniaSoftwareok) · [Portfolio](https://ingeniasoftwareok.github.io/portfolio/) · [Email](mailto:ingeniasoftwareok@gmail.com)
