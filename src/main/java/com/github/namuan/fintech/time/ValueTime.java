package com.github.namuan.fintech.time;
import java.time.Instant;
public record ValueTime(Instant value) { public ValueTime { if (value == null) throw new IllegalArgumentException("value is required"); } }
