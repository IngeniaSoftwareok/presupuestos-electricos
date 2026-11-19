package ar.com.ingenia.presupuestos.dominio;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Presupuestos {
    private Presupuestos() {}

    public record Cliente(String codigo, String nombre, String localidad) {}

    public record Concepto(String codigo, String nombre, String unidad, String rubro,
                           @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal precio) {}

    public record Datos(List<Cliente> clientes, List<Concepto> conceptos) {}

    public record RenglonPedido(String concepto, BigDecimal cantidad) {}

    public record Pedido(String cliente, BigDecimal descuento, List<RenglonPedido> renglones) {}

    public record Renglon(String concepto, String nombre, String unidad,
                          @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal cantidad,
                          @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal precio,
                          @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal importe) {}

    public record Resumen(UUID id, Cliente cliente, Instant fecha,
                          @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total) {}

    public record Detalle(UUID id, Cliente cliente, Instant fecha,
                          @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal descuentoPorcentaje,
                          @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal subtotal,
                          @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal descuento,
                          @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total,
                          List<Renglon> renglones) {}

    public record Pagina(List<Resumen> presupuestos, String siguiente) {}

    public record Guardado(Detalle presupuesto, boolean nuevo) {}
}
