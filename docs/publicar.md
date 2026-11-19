# Publicar la edición web

El enlace de la demo abre directamente el editor. Esta edición usa HTTPS, se sirve como archivos estáticos y guarda cada historial en el navegador que lo creó.

Para publicar una copia en GitHub Pages desde este repositorio, elegí **GitHub Actions** como fuente en **Settings → Pages** y ejecutá **Publicar la edición web en GitHub Pages** desde **Actions**. El workflow incluye la ruta base del repositorio y publica la carpeta que genera Next.js.

Para generar esos archivos desde `web/`, usá estos comandos con Node 24.

```sh
npm ci
npm run build:web
```

El resultado queda en `web/out/`. Servilo en la raíz de un sitio HTTPS o establecé `DEMO_BASE_PATH` antes de compilar si lo vas a publicar bajo una subruta.
