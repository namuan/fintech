package com.github.namuan.fintech.time;

import java.time.Instant;
import java.util.Optional;

public record FinancialTimestampSet(Instant valueTime, Instant bookingTime, Optional<Instant> settlementTime) {
    public FinancialTimestampSet {
        if (valueTime == null) throw new IllegalArgumentException("valueTime is required");
        if (bookingTime == null) throw new IllegalArgumentException("bookingTime is required");
        settlementTime = settlementTime == null ? Optional.empty() : settlementTime;
    }
    public static FinancialTimestampSet bookedNow(Instant valueTime) { return new FinancialTimestampSet(valueTime, Instant.now(), Optional.empty()); }
}
