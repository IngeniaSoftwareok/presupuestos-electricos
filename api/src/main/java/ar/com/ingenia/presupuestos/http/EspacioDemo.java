package ar.com.ingenia.presupuestos.http;

import ar.com.ingenia.presupuestos.dominio.Huella;
import ar.com.ingenia.presupuestos.dominio.Problema;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
class EspacioDemo {
    private static final Pattern FORMATO = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final SecureRandom AZAR = new SecureRandom();
    private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();

    private final String origen;
    private final boolean segura;
    private final String nombre;

    EspacioDemo(@Value("${demo.origen}") String origen, @Value("${demo.cookie-segura}") boolean segura) {
        this.origen = origen;
        this.segura = segura;
        this.nombre = segura ? "__Host-taller_espacio" : "taller_espacio";
    }

    void iniciar(HttpServletRequest solicitud, HttpServletResponse respuesta) {
        if (token(solicitud).isPresent()) {
            return;
        }
        byte[] bytes = new byte[32];
        AZAR.nextBytes(bytes);
        var cookie = ResponseCookie.from(nombre, CODIFICADOR.encodeToString(bytes))
                .httpOnly(true).secure(segura).sameSite("Strict").path("/").maxAge(Duration.ofDays(7)).build();
        respuesta.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    byte[] exigir(HttpServletRequest solicitud) {
        // En la base guardo la huella. El valor que da acceso queda en la cookie del navegador.
        return token(solicitud).map(Huella::sha256).orElseThrow(() ->
                new Problema(Problema.Tipo.ESPACIO_REQUERIDO, "Volvé a abrir la demo para iniciar tu espacio."));
    }

    void comprobarOrigen(HttpServletRequest solicitud) {
        if (!origen.equals(solicitud.getHeader(HttpHeaders.ORIGIN))) {
            throw new Problema(Problema.Tipo.ORIGEN_INVALIDO, "Abrí la demo desde su dirección habitual.");
        }
    }

    private Optional<String> token(HttpServletRequest solicitud) {
        Cookie[] cookies = solicitud.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies).filter(cookie -> nombre.equals(cookie.getName()))
                .map(Cookie::getValue).filter(valor -> FORMATO.matcher(valor).matches()).findFirst();
    }
}
