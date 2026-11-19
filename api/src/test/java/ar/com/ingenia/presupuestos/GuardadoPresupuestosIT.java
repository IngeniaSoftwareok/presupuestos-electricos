package ar.com.ingenia.presupuestos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import ar.com.ingenia.presupuestos.persistencia.ArchivoPresupuestos;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "demo.origen=https://presupuestos.example",
        "demo.cookie-segura=true"
})
class GuardadoPresupuestosIT {
    private static final String ORIGEN = "https://presupuestos.example";
    private static final String PEDIDO = """
            {"cliente":"casa-laprida","descuento":"5","renglones":[
               {"concepto":"cable-25","cantidad":"18.5"},
               {"concepto":"termica","cantidad":"2"},
               {"concepto":"mano-obra","cantidad":"3"}]}
            """;

    private final HttpClient http = HttpClient.newHttpClient();
    @LocalServerPort int puerto;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean ArchivoPresupuestos archivo;

    @Test
    void guardaUnaSolaVezAunqueLleguenSeisEnviosJuntos() throws Exception {
        String cookie = espacio();
        UUID clave = UUID.randomUUID();
        var envios = new ArrayList<CompletableFuture<HttpResponse<String>>>();
        for (int intento = 0; intento < 6; intento++) {
            envios.add(http.sendAsync(post(cookie, clave, PEDIDO, ORIGEN), HttpResponse.BodyHandlers.ofString()));
        }
        CompletableFuture.allOf(envios.toArray(CompletableFuture[]::new)).join();
        var respuestas = envios.stream().map(CompletableFuture::join).toList();
        assertThat(respuestas).extracting(HttpResponse::statusCode).containsExactlyInAnyOrder(201, 200, 200, 200, 200, 200);
        JsonNode primero = cuerpo(respuestas.getFirst());
        assertThat(primero.path("total").asText()).isEqualTo("134828.75");
        assertThat(primero.path("total").isTextual()).isTrue();
        assertThat(primero.path("renglones").size()).isEqualTo(3);
        for (var respuesta : respuestas) {
            assertThat(cuerpo(respuesta)).isEqualTo(primero);
        }
        assertThat(cuerpo(get("/presupuestos", cookie)).path("presupuestos").size()).isEqualTo(1);
        assertThat(cuerpo(get("/presupuestos/" + primero.path("id").asText(), cookie))).isEqualTo(primero);
    }

    @Test
    void reconoceCambiosDeFormatoYRechazaUnPedidoDistintoConLaMismaClave() throws Exception {
        var cookie = espacio();
        var clave = UUID.randomUUID();
        var guardado = enviar(post(cookie, clave, PEDIDO, ORIGEN));
        var mismo = enviar(post(cookie, clave, PEDIDO.replace("\"18.5\"", "\"18.500\""), ORIGEN));
        var distinto = enviar(post(cookie, clave, PEDIDO.replace("\"18.5\"", "\"19\""), ORIGEN));
        assertThat(guardado.statusCode()).isEqualTo(201);
        assertThat(mismo.statusCode()).isEqualTo(200);
        assertThat(cuerpo(mismo)).isEqualTo(cuerpo(guardado));
        assertThat(distinto.statusCode()).isEqualTo(409);
        assertThat(cuerpo(distinto).path("codigo").asText()).isEqualTo("CLAVE_REUTILIZADA");
    }

    @Test
    void soloDevuelvePresupuestosDelNavegadorQueLosGuardo() throws Exception {
        var primero = espacio();
        var segundo = espacio();
        var respuesta = enviar(post(primero, UUID.randomUUID(), PEDIDO, ORIGEN));
        var id = cuerpo(respuesta).path("id").asText();
        assertThat(get("/presupuestos/" + id, primero).statusCode()).isEqualTo(200);
        assertThat(get("/presupuestos/" + id, segundo).statusCode()).isEqualTo(404);
        assertThat(cuerpo(get("/presupuestos", segundo)).path("presupuestos").isEmpty()).isTrue();
        assertThat(get("/presupuestos", null).statusCode()).isEqualTo(403);
        assertThat(enviar(post(primero, UUID.randomUUID(), PEDIDO, "https://otro.example")).statusCode())
                .isEqualTo(403);
    }

    @Test
    void indicaElCampoInvalidoYCalculaConLosPreciosDelCatalogo() throws Exception {
        var cookie = espacio();
        var error = enviar(post(cookie, UUID.randomUUID(), PEDIDO.replace("\"18.5\"", "\"0\""), ORIGEN));
        assertThat(error.statusCode()).isEqualTo(400);
        assertThat(cuerpo(error).path("campos").path("renglones[0].cantidad").asText()).contains("mayor a cero");

        var precioEnviado = enviar(post(cookie, UUID.randomUUID(), PEDIDO.replace("\"18.5\"", "\"18.5\",\"precio\":\"1\""), ORIGEN));
        assertThat(precioEnviado.statusCode()).isEqualTo(400);
        var duplicado = enviar(post(cookie, UUID.randomUUID(), PEDIDO.replace("\"5\"", "\"5\",\"descuento\":\"0\""), ORIGEN));
        assertThat(duplicado.statusCode()).isEqualTo(400);
        var nulo = enviar(post(cookie, UUID.randomUUID(), """
                {"cliente":"casa-laprida","descuento":"0","renglones":[null]}
                """, ORIGEN));
        assertThat(nulo.statusCode()).isEqualTo(400);
        assertThat(cuerpo(get("/presupuestos", cookie)).path("presupuestos").isEmpty()).isTrue();
    }

    @Test
    void conservaElPrecioDeVentaAunqueCambieElCatalogo() throws Exception {
        var cookie = espacio();
        var clave = UUID.randomUUID();
        var guardado = cuerpo(enviar(post(cookie, clave, PEDIDO, ORIGEN)));
        var id = guardado.path("id").asText();
        jdbc.update("UPDATE concepto SET precio = ? WHERE codigo = ?", new BigDecimal("2000.00"), "cable-25");
        try {
            var reabierto = cuerpo(get("/presupuestos/" + id, cookie));
            assertThat(reabierto).isEqualTo(guardado);
            assertThat(cuerpo(enviar(post(cookie, clave, PEDIDO, ORIGEN)))).isEqualTo(guardado);
            var nuevo = cuerpo(enviar(post(cookie, UUID.randomUUID(), PEDIDO, ORIGEN)));
            assertThat(nuevo.path("renglones").get(0).path("precio").asText()).isEqualTo("2000.00");
        } finally {
            jdbc.update("UPDATE concepto SET precio = ? WHERE codigo = ?", new BigDecimal("1450.00"), "cable-25");
        }
    }

    @Test
    void revierteLaCabeceraSiFallaElGuardadoDeLosRenglones() throws Exception {
        var cookie = espacio();
        var clave = UUID.randomUUID();
        doThrow(new IllegalStateException("Fallo de prueba al guardar los renglones.")).doCallRealMethod()
                .when(archivo).guardarRenglones(anyLong(), anyList());
        assertThat(enviar(post(cookie, clave, PEDIDO, ORIGEN)).statusCode()).isEqualTo(500);
        assertThat(cuerpo(get("/presupuestos", cookie)).path("presupuestos").isEmpty()).isTrue();
        assertThat(enviar(post(cookie, clave, PEDIDO, ORIGEN)).statusCode()).isEqualTo(201);
    }

    @Test
    void paginaDesdeElUltimoResultadoSinRepetirPresupuestos() throws Exception {
        var cookie = espacio();
        for (int item = 0; item < 17; item++) {
            assertThat(enviar(post(cookie, UUID.randomUUID(), PEDIDO, ORIGEN)).statusCode()).isEqualTo(201);
        }
        var primera = cuerpo(get("/presupuestos", cookie));
        var segunda = cuerpo(get("/presupuestos?antesDe=" + primera.path("siguiente").asText(), cookie));
        var ids = new ArrayList<String>();
        for (JsonNode pagina : new JsonNode[]{primera, segunda}) {
            StreamSupport.stream(pagina.path("presupuestos").spliterator(), false)
                    .map(fila -> fila.path("id").asText()).forEach(ids::add);
        }
        assertThat(primera.path("presupuestos").size()).isEqualTo(15);
        assertThat(segunda.path("presupuestos").size()).isEqualTo(2);
        assertThat(segunda.path("siguiente").isNull()).isTrue();
        assertThat(ids).hasSize(17).doesNotHaveDuplicates();
    }

    private String espacio() throws Exception {
        var respuesta = get("/datos", null);
        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.headers().firstValue("Cache-Control")).contains("no-store");
        var cookie = respuesta.headers().firstValue("Set-Cookie").orElseThrow();
        assertThat(cookie).startsWith("__Host-taller_espacio=").contains("Path=/", "Secure", "HttpOnly", "SameSite=Strict");
        return cookie.split(";", 2)[0];
    }

    private HttpResponse<String> get(String ruta, String cookie) throws Exception {
        var peticion = solicitud(ruta);
        if (cookie != null) peticion.header("Cookie", cookie);
        return enviar(peticion.GET().build());
    }

    private HttpRequest post(String cookie, UUID clave, String contenido, String origen) {
        return solicitud("/presupuestos").header("Cookie", cookie).header("Origin", origen)
                .header("Idempotency-Key", clave.toString()).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(contenido)).build();
    }

    private HttpRequest.Builder solicitud(String ruta) {
        return HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api" + ruta))
                .timeout(Duration.ofSeconds(15));
    }

    private HttpResponse<String> enviar(HttpRequest peticion) throws Exception {
        return http.send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode cuerpo(HttpResponse<String> respuesta) throws Exception {
        return json.readTree(respuesta.body());
    }
}
