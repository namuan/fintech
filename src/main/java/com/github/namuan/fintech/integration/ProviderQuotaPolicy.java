package com.github.namuan.fintech.integration;
import java.time.Duration;
public record ProviderQuotaPolicy(long maxCalls, Duration per) {}
