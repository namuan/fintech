package com.github.namuan.fintech.money;

import java.math.BigDecimal;
import java.util.Objects;

public record DecimalAmount(BigDecimal value) implements Amount {
    public DecimalAmount {
        Objects.requireNonNull(value, "value");
    }

    public static DecimalAmount parse(String value) {
        return new DecimalAmount(new BigDecimal(value));
    }

    @Override public BigDecimal toBigDecimal() { return value; }
    @Override public int scale() { return Math.max(value.scale(), 0); }
}
