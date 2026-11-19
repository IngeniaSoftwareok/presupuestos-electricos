package ar.com.ingenia.presupuestos.aplicacion;

import ar.com.ingenia.presupuestos.dominio.Huella;
import ar.com.ingenia.presupuestos.dominio.Importes;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Detalle;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Guardado;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Pagina;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Pedido;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.Renglon;
import ar.com.ingenia.presupuestos.dominio.Presupuestos.RenglonPedido;
import ar.com.ingenia.presupuestos.dominio.Problema;
import ar.com.ingenia.presupuestos.persistencia.ArchivoPresupuestos;
import ar.com.ingenia.presupuestos.persistencia.ArchivoPresupuestos.Registro;
import ar.com.ingenia.presupuestos.persistencia.Catalogo;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionPresupuestos {
    private final Catalogo catalogo;
    private final ArchivoPresupuestos archivo;

    public GestionPresupuestos(Catalogo catalogo, ArchivoPresupuestos archivo) {
        this.catalogo = catalogo;
        this.archivo = archivo;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Guardado guardar(byte[] espacio, UUID clave, Pedido pedido) {
        byte[] huella = huella(pedido);
        var anterior = archivo.porClave(espacio, clave);
        if (anterior.isPresent()) {
            return recuperar(anterior.get(), huella);
        }

        var cliente = catalogo.cliente(pedido.cliente())
                .orElseThrow(() -> Problema.validacion("cliente", "Elegí un cliente de la lista."));
        var codigos = pedido.renglones().stream().map(RenglonPedido::concepto).distinct().toList();
        var conceptos = catalogo.conceptos(codigos);
        var renglones = IntStream.range(0, pedido.renglones().size()).mapToObj(posicion -> {
            var renglon = pedido.renglones().get(posicion);
            var concepto = conceptos.get(renglon.concepto());
            if (concepto == null) {
                throw Problema.validacion("renglones[" + posicion + "].concepto", "Elegí un concepto del catálogo.");
            }
            return new Renglon(concepto.codigo(), concepto.nombre(), concepto.unidad(), renglon.cantidad(),
                    concepto.precio(), Importes.renglon(renglon.cantidad(), concepto.precio()));
        }).toList();
        var totales = Importes.sumar(renglones.stream().map(Renglon::importe).toList(), pedido.descuento());

        UUID id = UUID.randomUUID();
        archivo.reservar(espacio, clave, id, huella, cliente, pedido.descuento(), totales);
        var registro = archivo.porClave(espacio, clave).orElseThrow();
        if (!id.equals(registro.id())) {
            return recuperar(registro, huella);
        }

        // Guardo los precios con el presupuesto para poder abrirlo aunque cambie el catálogo.
        archivo.guardarRenglones(registro.secuencia(), renglones);
        return new Guardado(registro.detalle(renglones), true);
    }

    @Transactional(readOnly = true)
    public Detalle detalle(byte[] espacio, UUID id) {
        var registro = archivo.porId(espacio, id).orElseThrow(() ->
                new Problema(Problema.Tipo.NO_ENCONTRADO, "Ese presupuesto no está en este historial."));
        return registro.detalle(archivo.renglones(registro.secuencia()));
    }

    public Pagina historial(byte[] espacio, Long antesDe) {
        return archivo.pagina(espacio, antesDe == null ? Long.MAX_VALUE : antesDe);
    }

    private Guardado recuperar(Registro registro, byte[] huella) {
        if (!MessageDigest.isEqual(registro.huella(), huella)) {
            throw new Problema(Problema.Tipo.CLAVE_REUTILIZADA,
                    "Este intento corresponde a otro presupuesto. Revisá el historial antes de volver a guardar.");
        }
        return new Guardado(registro.detalle(archivo.renglones(registro.secuencia())), false);
    }

    private static byte[] huella(Pedido pedido) {
        // Las cantidades ya vienen normalizadas: 2, 2.0 y 2.000 representan el mismo pedido.
        var items = pedido.renglones().stream()
                .map(renglon -> renglon.concepto() + ":" + renglon.cantidad().toPlainString())
                .collect(Collectors.joining("|"));
        return Huella.sha256(pedido.cliente() + "|" + pedido.descuento().toPlainString() + "|" + items);
    }
}
