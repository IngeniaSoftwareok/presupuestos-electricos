package ar.com.ingenia.quote.io;

import ar.com.ingenia.quote.domain.QuoteLine;
import ar.com.ingenia.quote.domain.QuoteResult;
import java.util.stream.Collectors;

/** Escribe el resultado como JSON con importes decimales en texto. */
public final class QuoteJsonWriter {
    public String write(QuoteResult r) {
        String lines = r.quote().lines().stream().map(this::line).collect(Collectors.joining(",\n"));
        return "{\n  \"demo\": true,\n  \"lines\": [\n" + lines + "\n  ],\n"
                + "  \"subtotal\": \"" + r.subtotal().toPlainString() + "\",\n"
                + "  \"discountPercent\": \"" + r.quote().discountPercent().toPlainString() + "\",\n"
                + "  \"discountAmount\": \"" + r.discountAmount().toPlainString() + "\",\n"
                + "  \"surchargePercent\": \"" + r.quote().surchargePercent().toPlainString() + "\",\n"
                + "  \"surchargeAmount\": \"" + r.surchargeAmount().toPlainString() + "\",\n"
                + "  \"total\": \"" + r.total().toPlainString() + "\"\n}";
    }

    private String line(QuoteLine l) {
        return "    {\"code\": \"" + escape(l.code()) + "\", \"description\": \"" + escape(l.description())
                + "\", \"quantity\": \"" + l.quantity().toPlainString() + "\", \"unitPrice\": \""
                + l.unitPrice().toPlainString() + "\", \"amount\": \"" + l.amount().toPlainString() + "\"}";
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
