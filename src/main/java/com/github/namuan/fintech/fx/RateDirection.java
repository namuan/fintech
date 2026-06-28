package com.github.namuan.fintech.fx;
import com.github.namuan.fintech.currency.AssetId;
public record RateDirection(AssetId from, AssetId to) { public RateDirection { if (from.equals(to)) throw new IllegalArgumentException("FX direction needs two distinct assets"); } }
