package com.github.namuan.fintech.trading;
import com.github.namuan.fintech.currency.AssetId;
public record Instrument(String symbol, AssetId baseAsset, AssetId quoteAsset) {}
