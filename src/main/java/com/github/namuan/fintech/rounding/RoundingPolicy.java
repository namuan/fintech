package com.github.namuan.fintech.rounding;

import com.github.namuan.fintech.money.DecimalAmount;
import com.github.namuan.fintech.money.Money;
import java.math.RoundingMode;
import java.util.Optional;

public record RoundingPolicy(String id, int scale, RoundingMode mode) {
    public RoundingPolicy {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        if (scale < 0) throw new IllegalArgumentException("scale must be non-negative");
        if (mode == null) throw new IllegalArgumentException("mode is required");
    }

    public RoundingResult apply(Money money, RoundingContext context) {
        var original = money.decimalValue();
        var roundedValue = original.setScale(scale, mode);
        Money rounded = new Money(new DecimalAmount(roundedValue), money.asset());
        var diff = original.subtract(roundedValue);
        if (diff.signum() == 0) return RoundingResult.exact(rounded);
        return new RoundingResult(rounded, Optional.of(new Residual(new Money(new DecimalAmount(diff), money.asset()), context.purpose())));
    }
}
