package com.github.namuan.fintech.fx;
import com.github.namuan.fintech.money.DecimalAmount;
import com.github.namuan.fintech.money.Money;
import com.github.namuan.fintech.rounding.RoundingContext;
import com.github.namuan.fintech.rounding.RoundingPolicy;
public record TransactionalConversion(Money source, Money target, FxQuote quote) {
    public static TransactionalConversion convert(Money source, FxQuote quote, RoundingPolicy rounding) {
        if (!source.asset().equals(quote.rate().direction().from())) throw new IllegalArgumentException("source asset does not match quote direction");
        var raw = source.decimalValue().multiply(quote.rate().rate());
        var rounded = rounding.apply(new Money(new DecimalAmount(raw), quote.rate().direction().to()), new RoundingContext("fx-conversion", java.util.Map.of("quoteId", quote.quoteId()))).rounded();
        return new TransactionalConversion(source, rounded, quote);
    }
}
