package ar.com.ingenia.quote.io;

import ar.com.ingenia.quote.domain.QuoteLine;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** CSV UTF-8 con punto decimal y separador ';'. No admite campos de varias líneas. */
public final class QuoteCsvReader {
    private static final List<String> HEADER = List.of("codigo", "descripcion", "cantidad", "precio_unitario");

    public List<QuoteLine> read(Reader source) throws IOException {
        BufferedReader reader = source instanceof BufferedReader b ? b : new BufferedReader(source);
        String header = reader.readLine();
        if (header == null || !fields(header.replaceFirst("^\uFEFF", "")).equals(HEADER)) {
            throw new IllegalArgumentException("Cabecera requerida: codigo;descripcion;cantidad;precio_unitario");
        }
        List<QuoteLine> lines = new ArrayList<>();
        String row;
        int number = 1;
        while ((row = reader.readLine()) != null) {
            number++;
            if (row.isBlank()) continue;
            try {
                List<String> f = fields(row);
                if (f.size() != 4) throw new IllegalArgumentException("Se requieren cuatro columnas.");
                if (!f.get(2).matches("[0-9]+(?:\\.[0-9]+)?") || !f.get(3).matches("[0-9]+(?:\\.[0-9]+)?")) {
                    throw new IllegalArgumentException("Usar números no negativos con punto decimal, sin separador de miles.");
                }
                lines.add(new QuoteLine(f.get(0), f.get(1), new BigDecimal(f.get(2)), new BigDecimal(f.get(3))));
                if (lines.size() > 200) throw new IllegalArgumentException("Máximo: 200 líneas.");
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Fila " + number + ": " + e.getMessage(), e);
            }
        }
        return List.copyOf(lines);
    }

    private List<String> fields(String row) {
        List<String> result = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        boolean closed = false;
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < row.length() && row.charAt(i + 1) == '"') { value.append('"'); i++; }
                else if (c == '"') { quoted = false; closed = true; }
                else value.append(c);
            } else if (c == ';') {
                result.add(value.toString().strip()); value.setLength(0); closed = false;
            } else if (c == '"' && value.isEmpty() && !closed) {
                quoted = true;
            } else if (closed && Character.isWhitespace(c)) {
                // Permite espacios entre el cierre de comillas y el separador.
            } else if (closed || c == '"') {
                throw new IllegalArgumentException("Comillas mal ubicadas.");
            } else value.append(c);
        }
        if (quoted) throw new IllegalArgumentException("Campo entre comillas sin cierre.");
        result.add(value.toString().strip());
        return result;
    }
}
