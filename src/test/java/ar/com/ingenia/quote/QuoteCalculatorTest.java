package ar.com.ingenia.quote;

import ar.com.ingenia.quote.domain.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class QuoteCalculatorTest {
    private static BigDecimal n(String value) { return new BigDecimal(value); }
    private static QuoteLine line(String q, String p) { return new QuoteLine("D-1", "Concepto ficticio", n(q), n(p)); }

    @Test void calculatesDiscountThenSurchargeWithDecimalMoney() {
        var quote = new Quote(List.of(line("2.5", "1200"), line("3", "450.50")), n("10"), n("5"));
        var result = new QuoteCalculator().calculate(quote);
        assertEquals(n("4351.50"), result.subtotal());
        assertEquals(n("435.15"), result.discountAmount());
        assertEquals(n("195.82"), result.surchargeAmount());
        assertEquals(n("4112.17"), result.total());
    }

    @Test void roundsEachLineBeforeAggregation() {
        var result = new QuoteCalculator().calculate(new Quote(List.of(line("0.001", "1005"), line("0.001", "1005")), n("0"), n("0")));
        assertEquals(n("2.02"), result.total());
    }

    @Test void fullDiscountLeavesNoBaseForSurcharge() {
        var result = new QuoteCalculator().calculate(new Quote(List.of(line("1", "0.05")), n("100"), n("100")));
        assertEquals(n("0.00"), result.total());
        assertEquals(n("0.00"), result.surchargeAmount());
    }

    @Test void lineOrderDoesNotChangeTheCommercialTotal() {
        var lines = new ArrayList<>(List.of(line("0.333", "12.25"), line("10", "4.99"), line("1.5", "500")));
        var first = new QuoteCalculator().calculate(new Quote(lines, n("7.25"), n("3.50")));
        Collections.reverse(lines);
        var second = new QuoteCalculator().calculate(new Quote(lines, n("7.25"), n("3.50")));
        assertEquals(first.total(), second.total());
        assertEquals(first.subtotal().subtract(first.discountAmount()).add(first.surchargeAmount()), first.total());
    }

    @Test void callerCannotRewriteAnExistingQuoteByMutatingItsList() {
        var source = new ArrayList<>(List.of(line("2", "10")));
        var quote = new Quote(source, n("0"), n("0"));
        source.clear();
        assertEquals(n("20.00"), new QuoteCalculator().calculate(quote).total());
        assertThrows(UnsupportedOperationException.class, () -> quote.lines().clear());
    }

    @ParameterizedTest @ValueSource(strings={"-1", "100.01", "1.001"})
    void rejectsInvalidAdjustmentWithoutSilentRounding(String percentage) {
        assertThrows(IllegalArgumentException.class, () -> new Quote(List.of(line("1", "10")), n(percentage), n("0")));
    }

    @Test void rejectsAmountsThatWouldNeedInputTruncation() {
        assertThrows(IllegalArgumentException.class, () -> line("1.0001", "10"));
        assertThrows(IllegalArgumentException.class, () -> line("1", "10.001"));
        assertThrows(IllegalArgumentException.class, () -> line("0", "10"));
        assertThrows(IllegalArgumentException.class, () -> line("1", "-10"));
    }
}
