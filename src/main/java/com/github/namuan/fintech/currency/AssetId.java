package com.github.namuan.fintech.currency;

/** Marker for anything that can denominate money: fiat currency, crypto token, instrument cash unit. */
public sealed interface AssetId permits FiatCurrency, CryptoAsset, CurrencyUnit {
    String code();
    int scale();
    String displayName();
}
