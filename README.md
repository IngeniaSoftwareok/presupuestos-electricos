# Demo de presupuestos en Java

Una muestra pequeña y ejecutable de cálculo de importes, validación y separación de entrada/salida. **Preparada para este portfolio con asistencia de IA**: no es el código de Newblock ni una versión de un producto comercial.

[![Pruebas Java](https://github.com/IngeniaSoftwareok/demo-presupuestos-java/actions/workflows/verify.yml/badge.svg)](https://github.com/IngeniaSoftwareok/demo-presupuestos-java/actions/workflows/verify.yml)

## Qué hace

Lee conceptos de un CSV, calcula sus importes, aplica un descuento y un recargo de demostración y escribe un resultado JSON. No inicia servicios, no se conecta a una base y no modifica el archivo de entrada.

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

El recargo es una regla de demostración, no un cálculo fiscal. No hay facturación electrónica, impuestos reglamentarios, PDF, usuarios, persistencia o una interfaz gráfica.

## Estructura

```text
src/main/java/ar/com/ingenia/quote/
  domain/    Entradas inmutables, reglas y resultado
  io/        Lectura CSV y escritura JSON
  cli/       Argumentos, archivos y códigos de salida
src/test/    Casos de negocio y entrada/salida
examples/    Datos ficticios y resultado reproducible
```

El dominio no depende de archivos, frameworks o servicios externos. La ejecución del JAR no necesita bibliotecas adicionales; JUnit es una dependencia de pruebas.

## Verificación

La preparación local ejecutó **15 casos de prueba sin fallos**. Cubren redondeo por línea, orden de descuento/recargo, descuento total, invariancia frente al orden de conceptos, conservación del snapshot, entrada CSV, texto escapado y rechazo de entradas sin emitir resultados parciales.

El workflow de GitHub Actions compila, prueba y ejecuta el ejemplo en Java 21. La primera ejecución remota terminó correctamente: [ver ejecución](https://github.com/IngeniaSoftwareok/demo-presupuestos-java/actions/runs/44332511792). El badge enlaza al estado de las ejecuciones del repositorio.

## Decisiones y límites

Se eligió un problema acotado que se pueda recorrer de principio a fin. La muestra permite revisar reglas de importes y adaptadores, no acredita por sí sola una plataforma empresarial, seguridad multiusuario o experiencia productiva de este programa.

[Agustin Crudo · Ingenia Software](https://github.com/IngeniaSoftwareok) · [Portfolio](https://ingeniasoftwareok.github.io/portfolio/) · [Email](mailto:ingeniasoftwareok@gmail.com)
