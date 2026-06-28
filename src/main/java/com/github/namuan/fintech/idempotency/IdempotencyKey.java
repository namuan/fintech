package com.github.namuan.fintech.idempotency;
public record IdempotencyKey(String value) { public IdempotencyKey { if (value == null || value.isBlank()) throw new IllegalArgumentException("key is required"); } }
