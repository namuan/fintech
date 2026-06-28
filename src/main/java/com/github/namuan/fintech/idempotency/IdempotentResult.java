package com.github.namuan.fintech.idempotency;
public record IdempotentResult<T>(T value, boolean replayed) {}
