# Idempotency

## What problem this package solves

In a distributed system you cannot guarantee exactly‑once delivery. The network can drop a response, the caller retries, and your service receives the same request twice. Without idempotency, that second delivery creates a duplicate payment, double‑debits an account, or mints money.

The `idempotency` package provides an `IdempotencyBarrier` that collapses duplicate deliveries into a single effect. It records the result of the first execution and replays it for every subsequent delivery of the same key.

## Package layout

```
com.github.namuan.fintech.idempotency
  IdempotencyKey        – a caller‑chosen unique key per operation
  IdempotencyScope      – (operation, actor, domain) — keys are scoped, not global
  IdempotentResult<T>   – (value, replayed: boolean) — tells the caller whether this was the first execution
  IdempotencyRecord     – stored record of a completed idempotent operation
  RepeatedPayloadPolicy – IGNORE or REQUIRE_SAME_HASH — what to do when the payload changes on retry
  IdempotencyStore      – find / putIfAbsent interface
  InMemoryIdempotencyStore – thread‑safe ConcurrentHashMap implementation
  IdempotencyBarrier    – the core synchronised gate: check store, run or replay
```

## What it deliberately does not solve

- The barrier is **synchronised** on the Java object monitor. This is correct for a single JVM. For a multi‑node deployment the barrier must be backed by a database with a unique constraint on `(scope, key)` — the JDBC adapter will provide that.
- It does **not** provide a time‑bounded deduplication window. The `InMemoryIdempotencyStore` keeps records forever. For production, a store backed by a TTL‑capable database is the caller’s choice.
- It does **not** decide whether a stored error should be replayed or whether the operation should be retried with a fresh key. The barrier replays the stored result (success or failure) as‑is. Callers can choose to retry with a new key for transient errors.

## Safe defaults

- Keys are **scoped** by `(domain, operation, actor)`. A `withdraw` key from user‑A cannot collide with a `withdraw` key from user‑B.
- The `putIfAbsent` semantic means the first caller wins. A concurrent duplicate will see the stored result.
- `RepeatedPayloadPolicy.REQUIRE_SAME_HASH` can be enabled to detect a caller who changes the payload on retry — a sign of a client bug or an attacker.
- The barrier works in‑memory out of the box for tests and single‑node services.

## Dangerous tradeoffs

| Tradeoff | Risk | Mitigation |
|---|---|---|
| Using `synchronized` in a multi‑node deployment | Two nodes can both pass `putIfAbsent` simultaneously | Use a database with `INSERT … ON CONFLICT DO NOTHING` + a unique index on (scope, key) |
| Accepting retries after a 24‑hour window | A genuine duplicate arriving on day 25 double‑counts | Prefer infinite retention unless you explicitly accept the correctness tradeoff |
| Storing the full result object in memory | Large results bloat the store; heap pressure under high load | Store a reference (e.g. a journal entry id) instead of the full object |
| Using business‑derived idempotency keys | Two genuine payments of the same amount look like a duplicate | Prefer explicit client‑generated `IdempotencyKey` values (UUIDs) |

## Example usage

```java
IdempotencyBarrier barrier = new IdempotencyBarrier(new InMemoryIdempotencyStore());
IdempotencyScope scope = new IdempotencyScope("withdraw", "user-42", "crypto");
IdempotencyKey key = new IdempotencyKey("req-abc-123");

IdempotentResult<String> result = barrier.execute(
    scope, key, "payload-hash",
    () -> {
        // The actual withdrawal logic — reserve, broadcast, wait for finality, post
        return "tx-0xdeadbeef";
    }
);

if (result.replayed()) {
    log.info("Returned stored result for {}", key);
}
return result.value(); // "tx-0xdeadbeef" — same every time
```

## Read next

- [Workflows](workflows.md) — how idempotency composes into multi‑step durable flows
- [Webhooks](webhooks.md) — applying idempotency to incoming webhook processing
- [Principles](principles.md) — the "no invented data" principle and why idempotency matters
