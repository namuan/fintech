package com.github.namuan.fintech.idempotency;
public record IdempotencyScope(String operation, String actor, String domain) {}
