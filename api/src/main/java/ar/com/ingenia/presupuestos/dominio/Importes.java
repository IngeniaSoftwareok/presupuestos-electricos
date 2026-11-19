package ar.com.ingenia.presupuestos.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class Importes {
    private Importes() {}

    public static BigDecimal renglon(BigDecimal cantidad, BigDecimal precio) {
        return cantidad.multiply(precio).setScale(2, RoundingMode.HALF_UP);
    }

    public static Totales sumar(List<BigDecimal> importes, BigDecimal porcentaje) {
        // Sumo los renglones ya redondeados para que el total coincida con lo que se muestra.
        BigDecimal subtotal = importes.stream().reduce(new BigDecimal("0.00"), BigDecimal::add);
        BigDecimal descuento = subtotal.multiply(porcentaje)
                .movePointLeft(2)
                .setScale(2, RoundingMode.HALF_UP);
        return new Totales(subtotal, descuento, subtotal.subtract(descuento));
    }

    public record Totales(BigDecimal subtotal, BigDecimal descuento, BigDecimal total) {}
}
