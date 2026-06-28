package com.github.namuan.fintech.quant;
import com.github.namuan.fintech.currency.AssetId;
public record ApproximateAmount(double value, AssetId asset, ApproximationPolicy policy) { public ApproximateAmount { if(!Double.isFinite(value)) throw new IllegalArgumentException("approximate amount must be finite"); } }
