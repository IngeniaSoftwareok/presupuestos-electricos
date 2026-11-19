package ar.com.ingenia.presupuestos.http;

import ar.com.ingenia.presupuestos.dominio.Problema;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class ErroresHttp {
    @ExceptionHandler(Problema.class)
    ProblemDetail problema(Problema error) {
        var estado = switch (error.tipo()) {
            case VALIDACION -> HttpStatus.BAD_REQUEST;
            case ORIGEN_INVALIDO, ESPACIO_REQUERIDO -> HttpStatus.FORBIDDEN;
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case CLAVE_REUTILIZADA -> HttpStatus.CONFLICT;
        };
        return respuesta(estado, error.tipo().name(), error.getMessage(), error.campos());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail campos(MethodArgumentNotValidException error) {
        Map<String, String> campos = new LinkedHashMap<>();
        error.getBindingResult().getFieldErrors().forEach(campo ->
                campos.putIfAbsent(campo.getField(), campo.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "VALIDACION", "Revisá los datos del presupuesto.", campos);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class,
            HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail formato() {
        return respuesta(HttpStatus.BAD_REQUEST, "FORMATO", "La solicitud tiene un formato inválido.", Map.of());
    }

    private static ProblemDetail respuesta(HttpStatus estado, String codigo, String mensaje, Map<String, String> campos) {
        var respuesta = ProblemDetail.forStatusAndDetail(estado, mensaje);
        respuesta.setProperty("codigo", codigo);
        if (!campos.isEmpty()) {
            respuesta.setProperty("campos", campos);
        }
        return respuesta;
    }
}
