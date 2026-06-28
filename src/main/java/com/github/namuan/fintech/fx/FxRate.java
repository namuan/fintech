package com.github.namuan.fintech.fx;
import java.math.BigDecimal;
import java.time.Instant;
public record FxRate(RateDirection direction, BigDecimal rate, Instant observedAt, RateSource source) { public FxRate { if (rate.signum() <= 0) throw new IllegalArgumentException("rate must be positive"); } }
