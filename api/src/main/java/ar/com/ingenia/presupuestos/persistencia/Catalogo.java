package ar.com.ingenia.presupuestos.persistencia;

import ar.com.ingenia.presupuestos.dominio.Presupuestos.Cliente;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Concepto;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Datos;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class Catalogo {
    private final JdbcClient jdbc;

    public Catalogo(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Datos datos() {
        var clientes = jdbc.sql("SELECT codigo, nombre, localidad FROM cliente ORDER BY nombre")
                .query((rs, fila) -> cliente(rs)).list();
        var conceptos = jdbc.sql("SELECT codigo, nombre, unidad, rubro, precio FROM concepto ORDER BY rubro, nombre")
                .query((rs, fila) -> concepto(rs)).list();
        return new Datos(clientes, conceptos);
    }

    public Optional<Cliente> cliente(String codigo) {
        return jdbc.sql("SELECT codigo, nombre, localidad FROM cliente WHERE codigo = :codigo")
                .param("codigo", codigo).query((rs, fila) -> cliente(rs)).optional();
    }

    public Map<String, Concepto> conceptos(Collection<String> codigos) {
        return jdbc.sql("SELECT codigo, nombre, unidad, rubro, precio FROM concepto WHERE codigo IN (:codigos)")
                .param("codigos", codigos)
                .query((rs, fila) -> concepto(rs)).list().stream()
                .collect(Collectors.toMap(Concepto::codigo, valor -> valor));
    }

    private static Cliente cliente(ResultSet rs) throws SQLException {
        return new Cliente(rs.getString("codigo"), rs.getString("nombre"), rs.getString("localidad"));
    }

    private static Concepto concepto(ResultSet rs) throws SQLException {
        return new Concepto(rs.getString("codigo"), rs.getString("nombre"), rs.getString("unidad"),
                rs.getString("rubro"), rs.getBigDecimal("precio"));
    }
}
