package ar.com.ingenia.presupuestos;

import static org.assertj.core.api.Assertions.assertThat;

import ar.com.ingenia.presupuestos.dominio.Importes;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImportesTest {
    @Test
    void redondeaCadaRenglonAntesDeSumar() {
        var importe = Importes.renglon(new BigDecimal("0.001"), new BigDecimal("1005.00"));
        assertThat(importe).isEqualTo(new BigDecimal("1.01"));
        assertThat(Importes.sumar(List.of(importe, importe), BigDecimal.ZERO).total())
                .isEqualTo(new BigDecimal("2.02"));
    }

    @Test
    void descuentaSobreElSubtotalConElMismoCriterioDeRedondeo() {
        var totales = Importes.sumar(List.of(new BigDecimal("0.05")), BigDecimal.TEN);
        assertThat(totales.descuento()).isEqualTo(new BigDecimal("0.01"));
        assertThat(totales.total()).isEqualTo(new BigDecimal("0.04"));
    }

    @Test
    void unDescuentoDelCienPorCientoDejaElTotalEnCero() {
        var totales = Importes.sumar(List.of(new BigDecimal("100.01")), new BigDecimal("100.00"));
        assertThat(totales.total()).isEqualTo(new BigDecimal("0.00"));
    }
}
