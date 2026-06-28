package com.github.namuan.fintech.time;
import java.time.LocalDate;
public record ReportingPeriod(LocalDate startInclusive, LocalDate endExclusive) {
    public boolean contains(LocalDate date) { return !date.isBefore(startInclusive) && date.isBefore(endExclusive); }
}
