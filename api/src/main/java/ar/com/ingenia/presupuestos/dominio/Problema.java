package ar.com.ingenia.presupuestos.dominio;

import java.util.Map;

public final class Problema extends RuntimeException {
    public enum Tipo {
        VALIDACION, ESPACIO_REQUERIDO, ORIGEN_INVALIDO, NO_ENCONTRADO, CLAVE_REUTILIZADA
    }

    private final Tipo tipo;
    private final Map<String, String> campos;

    public Problema(Tipo tipo, String mensaje) {
        this(tipo, mensaje, Map.of());
    }

    public Problema(Tipo tipo, String mensaje, Map<String, String> campos) {
        super(mensaje);
        this.tipo = tipo;
        this.campos = Map.copyOf(campos);
    }

    public static Problema validacion(String campo, String mensaje) {
        return new Problema(Tipo.VALIDACION, "Revisá los datos del presupuesto.", Map.of(campo, mensaje));
    }

    public Tipo tipo() {
        return tipo;
    }

    public Map<String, String> campos() {
        return campos;
    }
}
