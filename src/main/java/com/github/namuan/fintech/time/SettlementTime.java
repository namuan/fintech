package com.github.namuan.fintech.time;
import java.time.Instant;
public record SettlementTime(Instant value) { public SettlementTime { if (value == null) throw new IllegalArgumentException("value is required"); } }
