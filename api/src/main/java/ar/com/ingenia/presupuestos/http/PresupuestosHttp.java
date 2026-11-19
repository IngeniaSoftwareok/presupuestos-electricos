package ar.com.ingenia.presupuestos.http;

import ar.com.ingenia.presupuestos.aplicacion.GestionPresupuestos;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Datos;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Detalle;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Pagina;
import ar.com.ingenia.presupuestos.persistencia.Catalogo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
class PresupuestosHttp {
    private final EspacioDemo espacio;
    private final GestionPresupuestos gestion;
    private final Catalogo catalogo;

    PresupuestosHttp(EspacioDemo espacio, GestionPresupuestos gestion, Catalogo catalogo) {
        this.espacio = espacio;
        this.gestion = gestion;
        this.catalogo = catalogo;
    }

    @GetMapping("/salud")
    ResponseEntity<Void> salud() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/datos")
    Datos datos(HttpServletRequest solicitud, HttpServletResponse respuesta) {
        var datos = catalogo.datos();
        espacio.iniciar(solicitud, respuesta);
        return datos;
    }

    @PostMapping(value = "/presupuestos", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Detalle> guardar(@Valid @RequestBody SolicitudPresupuesto solicitud,
                                   @RequestHeader("Idempotency-Key") UUID clave, HttpServletRequest http) {
        espacio.comprobarOrigen(http);
        var guardado = gestion.guardar(espacio.exigir(http), clave, solicitud.aPedido());
        return guardado.nuevo()
                ? ResponseEntity.created(URI.create("/api/presupuestos/" + guardado.presupuesto().id()))
                        .body(guardado.presupuesto())
                : ResponseEntity.ok(guardado.presupuesto());
    }

    @GetMapping("/presupuestos")
    Pagina historial(@RequestParam(required = false) @Positive Long antesDe, HttpServletRequest http) {
        return gestion.historial(espacio.exigir(http), antesDe);
    }

    @GetMapping("/presupuestos/{id}")
    Detalle detalle(@PathVariable UUID id, HttpServletRequest http) {
        return gestion.detalle(espacio.exigir(http), id);
    }
}
