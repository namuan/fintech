package com.github.namuan.fintech.idempotency;
public record IdempotencyRecord(IdempotencyScope scope, IdempotencyKey key, String payloadHash, Object result) {}
