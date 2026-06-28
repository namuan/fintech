package com.github.namuan.fintech.payments;
import com.github.namuan.fintech.money.Money; import java.time.Instant;
public record Capture(String id, String authorizationId, Money amount, Instant capturedAt) {}
