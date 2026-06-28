package com.github.namuan.fintech.currency;

import java.util.Currency;
import java.util.Objects;

public record FiatCurrency(String code, int scale, String displayName) implements AssetId {
    public FiatCurrency {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(displayName, "displayName");
        code = code.toUpperCase();
        if (!code.matches("[A-Z]{3}")) throw new IllegalArgumentException("Fiat code must be ISO-like 3 uppercase letters");
        if (scale < 0) throw new IllegalArgumentException("scale must be non-negative");
    }

    public static FiatCurrency of(String code) {
        Currency currency = Currency.getInstance(code.toUpperCase());
        int digits = Math.max(currency.getDefaultFractionDigits(), 0);
        return new FiatCurrency(currency.getCurrencyCode(), digits, currency.getDisplayName());
    }
}
