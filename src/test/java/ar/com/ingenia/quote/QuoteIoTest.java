package ar.com.ingenia.quote;

import ar.com.ingenia.quote.cli.Main;
import ar.com.ingenia.quote.domain.*;
import ar.com.ingenia.quote.io.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class QuoteIoTest {
    @TempDir Path directory;
    private final String header="codigo;descripcion;cantidad;precio_unitario\n";

    @Test void readsQuotedSeparatorsEscapedQuotesAndBom() throws IOException {
        var lines=new QuoteCsvReader().read(new StringReader("\uFEFF"+header+"A;\"Instalacion; version \"\"demo\"\"\";1.500;10.50\n"));
        assertEquals("Instalacion; version \"demo\"",lines.getFirst().description());
        assertEquals(new BigDecimal("15.75"),lines.getFirst().amount());
    }

    @Test void rejectsMalformedCsvWithUsefulRowInformation() {
        var e=assertThrows(IllegalArgumentException.class,()->new QuoteCsvReader().read(new StringReader(header+"A;Trabajo;1,5;10.50\n")));
        assertTrue(e.getMessage().contains("Fila 2"));
        assertThrows(IllegalArgumentException.class,()->new QuoteCsvReader().read(new StringReader(header+"A;\"Sin cerrar;1;10\n")));
    }

    @Test void jsonEscapesUserTextAndKeepsMoneyAsDecimalStrings() throws IOException {
        var quote=new Quote(List.of(new QuoteLine("A", "Ruta \\ y \"texto\"", new BigDecimal("1"), new BigDecimal("0.10"))), BigDecimal.ZERO, BigDecimal.ZERO);
        String json=new QuoteJsonWriter().write(new QuoteCalculator().calculate(quote));
        assertTrue(json.contains("Ruta \\\\ y \\\"texto\\\""));
        assertTrue(json.contains("\"total\": \"0.10\""));
    }

    @Test void cliProducesAResultFromARealUtf8File() throws IOException {
        Path csv=directory.resolve("input.csv"); Files.writeString(csv,header+"A;Ejemplo;2;10.50\n",StandardCharsets.UTF_8);
        var out=new ByteArrayOutputStream(); var err=new ByteArrayOutputStream();
        int result=Main.run(new String[]{csv.toString(),"--descuento=10"},new PrintStream(out,true,StandardCharsets.UTF_8),new PrintStream(err));
        assertEquals(0,result); assertTrue(out.toString(StandardCharsets.UTF_8).contains("\"total\": \"18.90\""));
        assertEquals(0,err.size());
    }

    @Test void cliDoesNotEmitAPartialQuoteForBadInput() throws IOException {
        Path csv=directory.resolve("input.csv"); Files.writeString(csv,header+"A;Ejemplo;0;10\n");
        var out=new ByteArrayOutputStream(); var err=new ByteArrayOutputStream();
        assertEquals(2,Main.run(new String[]{csv.toString()},new PrintStream(out),new PrintStream(err)));
        assertEquals(0,out.size()); assertTrue(err.size()>0);
    }

    @Test void cliRejectsDuplicateOptionsInsteadOfSilentlyOverridingThem() throws IOException {
        Path csv=directory.resolve("input.csv"); Files.writeString(csv,header+"A;Ejemplo;1;10\n");
        assertEquals(2,Main.run(new String[]{csv.toString(),"--descuento=5","--descuento=10"},new PrintStream(new ByteArrayOutputStream()),new PrintStream(new ByteArrayOutputStream())));
    }
}
