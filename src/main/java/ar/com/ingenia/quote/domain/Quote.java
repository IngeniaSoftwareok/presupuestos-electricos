package ar.com.ingenia.quote.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Snapshot de entrada; modificar la lista original no cambia el presupuesto. */
public record Quote(List<QuoteLine> lines, BigDecimal discountPercent, BigDecimal surchargePercent) {
    public Quote {
        if (lines == null || lines.isEmpty() || lines.size() > 200 || lines.stream().anyMatch(l -> l == null)) {
            throw new IllegalArgumentException("El presupuesto necesita entre 1 y 200 lineas validas.");
        }
        lines = List.copyOf(lines);
        discountPercent = percentage(discountPercent, "descuento");
        surchargePercent = percentage(surchargePercent, "recargo");
    }

    private static BigDecimal percentage(BigDecimal value, String field) {
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El " + field + " debe estar entre 0 y 100.");
        }
        try {
            return value.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("El " + field + " admite hasta dos decimales.", e);
        }
    }
}
