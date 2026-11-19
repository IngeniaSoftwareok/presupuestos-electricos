package ar.com.ingenia.presupuestos.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class CabecerasApi extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta, FilterChain siguiente)
            throws ServletException, IOException {
        respuesta.setHeader("Cache-Control", "no-store");
        respuesta.setHeader("X-Content-Type-Options", "nosniff");
        siguiente.doFilter(solicitud, respuesta);
    }
}
