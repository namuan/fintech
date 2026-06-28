package com.github.namuan.fintech.idempotency;
import java.util.Optional;
public interface IdempotencyStore { Optional<IdempotencyRecord> find(IdempotencyScope scope, IdempotencyKey key); IdempotencyRecord putIfAbsent(IdempotencyRecord record); }
