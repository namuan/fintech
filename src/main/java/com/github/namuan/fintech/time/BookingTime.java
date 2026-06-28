package com.github.namuan.fintech.time;
import java.time.Instant;
public record BookingTime(Instant value) { public BookingTime { if (value == null) throw new IllegalArgumentException("value is required"); } }
