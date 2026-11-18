package ar.com.ingenia.quote.cli;

import ar.com.ingenia.quote.domain.Quote;
import ar.com.ingenia.quote.domain.QuoteCalculator;
import ar.com.ingenia.quote.io.QuoteCsvReader;
import ar.com.ingenia.quote.io.QuoteJsonWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        int status = run(args, System.out, System.err);
        if (status != 0) System.exit(status);
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) {
            out.println("Uso: java -jar demo-presupuestos-java-1.0.0.jar archivo.csv [--descuento=10] [--recargo=5]");
            return 0;
        }
        try {
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal surcharge = BigDecimal.ZERO;
            boolean discountSeen = false;
            boolean surchargeSeen = false;
            for (int i = 1; i < args.length; i++) {
                if (args[i].startsWith("--descuento=") && !discountSeen) {
                    discount = percentage(args[i].substring("--descuento=".length())); discountSeen = true;
                } else if (args[i].startsWith("--recargo=") && !surchargeSeen) {
                    surcharge = percentage(args[i].substring("--recargo=".length())); surchargeSeen = true;
                } else throw new IllegalArgumentException("Opcion desconocida o repetida: " + args[i]);
            }
            Path input = Path.of(args[0]);
            if (Files.size(input) > 262144) throw new IllegalArgumentException("El CSV debe ocupar hasta 256 KiB.");
            try (var reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
                Quote quote = new Quote(new QuoteCsvReader().read(reader), discount, surcharge);
                out.println(new QuoteJsonWriter().write(new QuoteCalculator().calculate(quote)));
            }
            return 0;
        } catch (IllegalArgumentException | IOException e) {
            err.println("No se pudo calcular el presupuesto: " + e.getMessage());
            return 2;
        }
    }

    private static BigDecimal percentage(String text) {
        if (!text.matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
            throw new IllegalArgumentException("Porcentaje invalido: usar punto decimal y hasta dos decimales.");
        }
        return new BigDecimal(text);
    }
}
