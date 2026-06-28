package com.github.namuan.fintech.money;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

public record ScaledAmount(BigInteger mantissa, int scale) implements Amount {
    public ScaledAmount {
        Objects.requireNonNull(mantissa, "mantissa");
        if (scale < 0) throw new IllegalArgumentException("scale must be non-negative");
    }

    public static ScaledAmount of(long mantissa, int scale) {
        return new ScaledAmount(BigInteger.valueOf(mantissa), scale);
    }

    public static ScaledAmount fromDecimal(BigDecimal decimal, int scale) {
        Objects.requireNonNull(decimal, "decimal");
        return new ScaledAmount(decimal.setScale(scale).unscaledValue(), scale);
    }

    @Override public BigDecimal toBigDecimal() {
        return new BigDecimal(mantissa, scale);
    }
}
