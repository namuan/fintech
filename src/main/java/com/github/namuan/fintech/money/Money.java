package com.github.namuan.fintech.money;

import com.github.namuan.fintech.currency.AssetId;
import java.math.BigDecimal;
import java.util.Objects;

public record Money(Amount amount, AssetId asset) {
    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(asset, "asset");
    }

    public static Money of(Amount amount, AssetId asset) { return new Money(amount, asset); }
    public static Money decimal(String value, AssetId asset) { return new Money(DecimalAmount.parse(value), asset); }
    public static Money minorUnits(long units, AssetId asset) { return new Money(ScaledAmount.of(units, asset.scale()), asset); }

    public BigDecimal decimalValue() { return amount.toBigDecimal(); }

    public Money plus(Money other) {
        requireSameAsset(other);
        return new Money(new DecimalAmount(decimalValue().add(other.decimalValue())), asset);
    }

    public Money minus(Money other) {
        requireSameAsset(other);
        return new Money(new DecimalAmount(decimalValue().subtract(other.decimalValue())), asset);
    }

    public Money negate() { return new Money(new DecimalAmount(decimalValue().negate()), asset); }

    public boolean isNegative() { return decimalValue().signum() < 0; }
    public boolean isZero() { return decimalValue().signum() == 0; }

    public void requireSameAsset(Money other) {
        Objects.requireNonNull(other, "other");
        if (!asset.equals(other.asset)) throw new IllegalArgumentException("Cross-asset arithmetic is forbidden: " + asset.code() + " vs " + other.asset.code());
    }
}
