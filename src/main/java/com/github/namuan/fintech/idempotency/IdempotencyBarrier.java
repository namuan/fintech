package com.github.namuan.fintech.idempotency;
import java.util.function.Supplier;
@SuppressWarnings("unchecked")
public final class IdempotencyBarrier {
    private final IdempotencyStore store;
    public IdempotencyBarrier(IdempotencyStore store) { this.store = store; }
    public synchronized <T> IdempotentResult<T> execute(IdempotencyScope scope, IdempotencyKey key, String payloadHash, Supplier<T> operation) {
        var existing = store.find(scope, key);
        if (existing.isPresent()) {
            if (existing.get().payloadHash() != null && payloadHash != null && !existing.get().payloadHash().equals(payloadHash)) throw new IllegalArgumentException("Repeated idempotency key with different payload");
            return new IdempotentResult<>((T) existing.get().result(), true);
        }
        T result = operation.get();
        store.putIfAbsent(new IdempotencyRecord(scope, key, payloadHash, result));
        return new IdempotentResult<>(result, false);
    }
}
