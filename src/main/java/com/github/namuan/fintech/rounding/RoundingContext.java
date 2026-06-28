package com.github.namuan.fintech.rounding;

import java.util.Map;

public record RoundingContext(String purpose, Map<String, String> labels) {
    public RoundingContext {
        labels = labels == null ? Map.of() : Map.copyOf(labels);
        if (purpose == null || purpose.isBlank()) throw new IllegalArgumentException("purpose is required");
    }
}
