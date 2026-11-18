package ar.com.ingenia.quote.domain;

import java.math.BigDecimal;

public record QuoteResult(Quote quote, BigDecimal subtotal, BigDecimal discountAmount,
                          BigDecimal surchargeAmount, BigDecimal total) {
}
