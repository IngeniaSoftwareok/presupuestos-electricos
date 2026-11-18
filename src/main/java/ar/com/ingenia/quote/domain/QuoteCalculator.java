package ar.com.ingenia.quote.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Sin archivos, frameworks ni estado compartido: solo reglas de esta demostracion. */
public final class QuoteCalculator {
    public QuoteResult calculate(Quote quote) {
        BigDecimal subtotal = quote.lines().stream().map(QuoteLine::amount)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        BigDecimal discount = percentageOf(subtotal, quote.discountPercent());
        BigDecimal afterDiscount = subtotal.subtract(discount);
        BigDecimal surcharge = percentageOf(afterDiscount, quote.surchargePercent());
        return new QuoteResult(quote, subtotal, discount, surcharge, afterDiscount.add(surcharge));
    }

    private BigDecimal percentageOf(BigDecimal amount, BigDecimal percentage) {
        return amount.multiply(percentage).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
    }
}
