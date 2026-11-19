package ar.com.ingenia.presupuestos.http;

import ar.com.ingenia.presupuestos.dominio.Presupuestos.Pedido;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.RenglonPedido;
import ar.com.ingenia.presupuestos.dominio.Problema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

record SolicitudPresupuesto(
        @NotNull(message = "Elegí un cliente de la lista.")
        @Pattern(regexp = "[a-z0-9-]{1,32}", message = "Elegí un cliente de la lista.") String cliente,
        @NotNull(message = "Ingresá un descuento entre 0 y 100.")
        @Pattern(regexp = "[0-9]{1,3}(?:\\.[0-9]{1,2})?", message = "Usá hasta dos decimales.") String descuento,
        @NotNull(message = "Agregá al menos un concepto.")
        @Size(min = 1, max = 20, message = "Agregá entre 1 y 20 conceptos.")
        List<@NotNull(message = "Completá este renglón.") @Valid Item> renglones
) {
    record Item(
            @NotNull(message = "Elegí un concepto del catálogo.")
            @Pattern(regexp = "[a-z0-9-]{1,32}", message = "Elegí un concepto del catálogo.") String concepto,
            @NotNull(message = "Ingresá una cantidad mayor a cero.")
            @Pattern(regexp = "[0-9]{1,4}(?:\\.[0-9]{1,3})?", message = "Usá hasta cuatro enteros y tres decimales.")
            String cantidad
    ) {}

    Pedido aPedido() {
        var porcentaje = new BigDecimal(descuento).setScale(2);
        if (porcentaje.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw Problema.validacion("descuento", "Ingresá un descuento entre 0 y 100.");
        }
        var items = IntStream.range(0, renglones.size()).mapToObj(posicion -> {
            var item = renglones.get(posicion);
            var cantidad = new BigDecimal(item.cantidad()).setScale(3);
            if (cantidad.signum() == 0) {
                throw Problema.validacion("renglones[" + posicion + "].cantidad", "Ingresá una cantidad mayor a cero.");
            }
            return new RenglonPedido(item.concepto(), cantidad);
        }).toList();
        return new Pedido(cliente, porcentaje, items);
    }
}
