package ar.com.ingenia.quote.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Una línea inmutable. Las cantidades tienen hasta tres decimales; los precios, dos. */
public record QuoteLine(String code, String description, BigDecimal quantity, BigDecimal unitPrice) {
    public QuoteLine {
        code = text(code, "código", 50);
        description = text(description, "descripción", 160);
        Objects.requireNonNull(quantity, "cantidad");
        Objects.requireNonNull(unitPrice, "precio unitario");
        if (quantity.signum() <= 0 || quantity.precision() > 16) {
            throw new IllegalArgumentException("La cantidad debe ser positiva y tener hasta 16 dígitos.");
        }
        if (unitPrice.signum() < 0 || unitPrice.precision() > 16) {
            throw new IllegalArgumentException("El precio debe ser no negativo y tener hasta 16 dígitos.");
        }
        try {
            quantity = quantity.setScale(3, RoundingMode.UNNECESSARY);
            unitPrice = unitPrice.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Cantidad: hasta 3 decimales; precio: hasta 2 decimales.", e);
        }
    }

    public BigDecimal amount() {
        return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    private static String text(String value, String field, int max) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Falta " + field + ".");
        String normalized = value.strip();
        if (normalized.length() > max || normalized.chars().anyMatch(c -> c < 32)) {
            throw new IllegalArgumentException("Texto inválido en " + field + ".");
        }
        return normalized;
    }
}
