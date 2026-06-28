package com.github.namuan.fintech.rounding;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RoundingModeRegistry {
    private final Map<String, RoundingPolicy> policies = new ConcurrentHashMap<>();

    public void register(RoundingPolicy policy) { policies.put(policy.id(), policy); }
    public Optional<RoundingPolicy> find(String id) { return Optional.ofNullable(policies.get(id)); }
    public RoundingPolicy require(String id) { return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown rounding policy: " + id)); }
}
