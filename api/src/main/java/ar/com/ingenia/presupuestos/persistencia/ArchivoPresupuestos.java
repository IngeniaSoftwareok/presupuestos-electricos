package ar.com.ingenia.presupuestos.persistencia;

import ar.com.ingenia.presupuestos.dominio.Importes.Totales;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Cliente;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Detalle;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Pagina;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Renglon;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Resumen;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ArchivoPresupuestos {
    private static final int TAMANO_PAGINA = 15;
    private static final String COLUMNAS = """
            secuencia, id, huella, cliente_codigo, cliente_nombre, cliente_localidad,
            fecha, descuento_porcentaje, subtotal, descuento, total
            """;

    private final JdbcClient jdbc;
    private final JdbcTemplate lote;

    public ArchivoPresupuestos(JdbcClient jdbc, JdbcTemplate lote) {
        this.jdbc = jdbc;
        this.lote = lote;
    }

    public Optional<Registro> porClave(byte[] espacio, UUID clave) {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM presupuesto WHERE espacio = :espacio AND clave = :clave")
                .param("espacio", espacio).param("clave", clave.toString())
                .query((rs, fila) -> registro(rs)).optional();
    }

    public Optional<Registro> porId(byte[] espacio, UUID id) {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM presupuesto WHERE espacio = :espacio AND id = :id")
                .param("espacio", espacio).param("id", id.toString())
                .query((rs, fila) -> registro(rs)).optional();
    }

    public void reservar(byte[] espacio, UUID clave, UUID id, byte[] huella, Cliente cliente,
                         BigDecimal porcentaje, Totales totales) {
        // La clave única deja que MySQL decida cuál de los dos envíos concurrentes entra primero.
        jdbc.sql("""
                INSERT INTO presupuesto (id, espacio, clave, huella, cliente_codigo, cliente_nombre,
                    cliente_localidad, descuento_porcentaje, subtotal, descuento, total)
                VALUES (:id, :espacio, :clave, :huella, :codigo, :nombre, :localidad,
                    :porcentaje, :subtotal, :descuento, :total)
                ON DUPLICATE KEY UPDATE secuencia = secuencia
                """)
                .param("id", id.toString()).param("espacio", espacio).param("clave", clave.toString())
                .param("huella", huella).param("codigo", cliente.codigo()).param("nombre", cliente.nombre())
                .param("localidad", cliente.localidad()).param("porcentaje", porcentaje)
                .param("subtotal", totales.subtotal()).param("descuento", totales.descuento())
                .param("total", totales.total()).update();
    }

    public void guardarRenglones(long secuencia, List<Renglon> renglones) {
        var argumentos = IntStream.range(0, renglones.size()).mapToObj(posicion -> {
            var renglon = renglones.get(posicion);
            return new Object[]{secuencia, posicion, renglon.concepto(), renglon.nombre(), renglon.unidad(),
                    renglon.cantidad(), renglon.precio(), renglon.importe()};
        }).toList();
        lote.batchUpdate("""
                INSERT INTO presupuesto_renglon (presupuesto_id, posicion, concepto_codigo, concepto_nombre,
                    unidad, cantidad, precio, importe) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, argumentos);
    }

    public List<Renglon> renglones(long secuencia) {
        return jdbc.sql("""
                SELECT concepto_codigo, concepto_nombre, unidad, cantidad, precio, importe
                FROM presupuesto_renglon WHERE presupuesto_id = :secuencia ORDER BY posicion
                """)
                .param("secuencia", secuencia)
                .query((rs, fila) -> new Renglon(rs.getString("concepto_codigo"), rs.getString("concepto_nombre"),
                        rs.getString("unidad"), rs.getBigDecimal("cantidad"), rs.getBigDecimal("precio"),
                        rs.getBigDecimal("importe"))).list();
    }

    public Pagina pagina(byte[] espacio, long antesDe) {
        var filas = jdbc.sql("""
                SELECT secuencia, id, cliente_codigo, cliente_nombre, cliente_localidad, fecha, total
                FROM presupuesto WHERE espacio = :espacio AND secuencia < :antesDe
                ORDER BY secuencia DESC LIMIT :limite
                """)
                .param("espacio", espacio).param("antesDe", antesDe).param("limite", TAMANO_PAGINA + 1)
                .query((rs, fila) -> new FilaHistorial(rs.getLong("secuencia"),
                        new Resumen(UUID.fromString(rs.getString("id")), cliente(rs), fecha(rs),
                                rs.getBigDecimal("total")))).list();
        var visibles = filas.stream().limit(TAMANO_PAGINA).map(FilaHistorial::resumen).toList();
        String siguiente = filas.size() > TAMANO_PAGINA
                ? Long.toString(filas.get(TAMANO_PAGINA - 1).secuencia()) : null;
        return new Pagina(visibles, siguiente);
    }

    private static Registro registro(ResultSet rs) throws SQLException {
        return new Registro(rs.getLong("secuencia"), UUID.fromString(rs.getString("id")),
                rs.getBytes("huella"), cliente(rs), fecha(rs), rs.getBigDecimal("descuento_porcentaje"),
                rs.getBigDecimal("subtotal"), rs.getBigDecimal("descuento"), rs.getBigDecimal("total"));
    }

    private static Cliente cliente(ResultSet rs) throws SQLException {
        return new Cliente(rs.getString("cliente_codigo"), rs.getString("cliente_nombre"),
                rs.getString("cliente_localidad"));
    }

    private static Instant fecha(ResultSet rs) throws SQLException {
        return rs.getTimestamp("fecha").toInstant();
    }

    public record Registro(long secuencia, UUID id, byte[] huella, Cliente cliente, Instant fecha,
                           BigDecimal porcentaje, BigDecimal subtotal, BigDecimal descuento, BigDecimal total) {
        public Detalle detalle(List<Renglon> renglones) {
            return new Detalle(id, cliente, fecha, porcentaje, subtotal, descuento, total, renglones);
        }
    }

    private record FilaHistorial(long secuencia, Resumen resumen) {}
}
