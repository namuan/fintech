package com.github.namuan.fintech.fx;
import java.time.Instant;
public record FxQuote(String quoteId, FxRate rate, Instant expiresAt) { public boolean isExpired(Instant now) { return !now.isBefore(expiresAt); } }
