package com.github.namuan.fintech.idempotency;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
public final class InMemoryIdempotencyStore implements IdempotencyStore {
    private final ConcurrentHashMap<String, IdempotencyRecord> records = new ConcurrentHashMap<>();
    public Optional<IdempotencyRecord> find(IdempotencyScope scope, IdempotencyKey key) { return Optional.ofNullable(records.get(composite(scope, key))); }
    public IdempotencyRecord putIfAbsent(IdempotencyRecord record) { return records.putIfAbsent(composite(record.scope(), record.key()), record); }
    private String composite(IdempotencyScope scope, IdempotencyKey key) { return scope.domain() + "|" + scope.operation() + "|" + scope.actor() + "|" + key.value(); }
}
