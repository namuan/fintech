# Storage semantics

## What problem this page solves

The in-memory stores in this repository are deliberately small and useful for tests, examples, and single-process experiments. JDBC/PostgreSQL adapters must not simply copy those APIs into tables. They must preserve the money-safety guarantees behind each store: append-only history, idempotent effects, durable workflow progress, atomic reservation checks, and at-least-once outbox delivery.

This page defines the storage contract that production adapters must satisfy before `Add JDBC/PostgreSQL adapters after storage semantics are finalized` can safely begin.

The repository now includes first-pass JDBC/PostgreSQL adapters under `com.github.namuan.fintech.storage.jdbc` and a PostgreSQL schema at `db/postgresql/schema.sql`. These adapters implement the store interfaces directly with `javax.sql.DataSource`; callers remain responsible for applying the schema and choosing transaction boundaries for larger cross-store units of work.

## Global adapter requirements

All durable adapters should follow these rules:

1. **No silent overwrite**
   - Financial facts are append-only unless the interface explicitly models a state transition.
   - Duplicate primary keys must be rejected or treated as an idempotent replay according to the store contract.

2. **Database constraints first**
   - Correctness must not rely only on Java-side checks.
   - Use primary keys, unique indexes, foreign keys where appropriate, check constraints, and transaction boundaries.

3. **Explicit transaction ownership**
   - Each adapter method must document whether it opens its own transaction or requires an ambient caller-managed transaction.
   - Cross-store operations, such as ledger posting plus outbox append, must be supported inside one database transaction.

4. **Precise value storage**
   - Store money as string decimal or mantissa/scale plus asset identity. Do not store accounting values as `double`, `float`, or JSON numbers.
   - Preserve scale where it carries domain meaning.

5. **Stable, replayable serialization**
   - Persist raw external inputs before parsing when the source is untrusted.
   - Store structured metadata in a versioned JSON representation, not as unversioned opaque Java serialization.

6. **Observable failures**
   - Constraint violations, serialization failures, lock timeouts, and deadlocks must be surfaced as explicit failures. They must not be swallowed and retried indefinitely without caller visibility.

## Transaction and isolation guidance

PostgreSQL adapters should prefer short transactions and row-level locking. Requirements differ by store:

| Store | Minimum semantic requirement | PostgreSQL pattern |
|---|---|---|
| Ledger | Append exactly once by `JournalEntryId`; balances derived from immutable postings | `INSERT` into entries/postings under one transaction; primary key on entry id |
| Idempotency | One operation wins per `(domain, operation, actor, key)`; all retries replay the stored result | Unique index; insert/reserve row atomically; lock row while completing result |
| Reservations | Available balance check and hold creation must be linearizable per account | Account-scoped advisory lock, row lock, or serializable transaction; held reservations queried in same transaction |
| Workflow | Persist progress before and after each step; resume sees the latest durable state | Upsert by workflow id; optionally optimistic version column |
| Raw webhooks | Store raw bytes before processing; duplicate provider event ids are deterministic | Primary key or unique index on provider + event id; byte payload column |
| Outbox | State change and publish intent are written atomically; publishing is at-least-once | Append outbox row in same transaction as state change; relay claims rows with `FOR UPDATE SKIP LOCKED` |
| Consumer deduplication | Consumer processes each stable event id at most once | Unique insert into consumed-event table |
| Audit | Append-only trail; no mutation of historical events | Insert-only table; optional hash chain; no update/delete in adapter API |

Use `READ COMMITTED` only when uniqueness constraints or explicit locks fully protect the invariant. Use stricter isolation or account/key-scoped locks when a read-then-write decision can otherwise race, especially for reservations.

## Store-specific contracts

### `LedgerStore`

Interface:

```java
void append(JournalEntry entry);
Optional<JournalEntry> find(JournalEntryId id);
List<JournalEntry> all();
```

Required semantics:

- `append` is atomic: either the journal entry and all postings are stored, or none are stored.
- A `JournalEntryId` can be appended only once.
- Stored entries and postings are immutable.
- `find` returns exactly the entry originally appended.
- `all` returns a complete view of the logical book, not necessarily in insertion order unless the adapter documents an order.
- Validation remains Java-side in `JournalEntry`, but the database should also enforce basic non-null, non-negative amount, asset identity, and side constraints.

Recommended PostgreSQL shape:

- `ledger_entries(id primary key, reason, timestamps..., metadata_json, created_at)`
- `ledger_postings(entry_id references ledger_entries(id), ordinal, account_id, side, amount_decimal_text or mantissa/scale, asset_code, asset_scale, memo, primary key(entry_id, ordinal))`

### `IdempotencyStore`

Required semantics:

- The composite key is `(scope.domain, scope.operation, scope.actor, key.value)`.
- At most one result is stored for a composite key.
- A retry with the same payload hash must receive the original result.
- A retry with a different payload hash must be rejected when payload validation is required by the barrier/caller.
- In a multi-node deployment, two concurrent callers must not both execute the protected side effect.

Recommended PostgreSQL pattern:

- Insert a placeholder row for the key before executing the protected operation, or claim the row with `INSERT ... ON CONFLICT DO NOTHING` followed by `SELECT ... FOR UPDATE`.
- Store status such as `IN_PROGRESS`, `SUCCEEDED`, `FAILED` if the adapter supports crashes during operation execution.
- Store result as a stable serialized payload or, preferably, a reference to the durable effect, such as a journal entry id or workflow id.

Open design decision before implementation:

- The current in-memory barrier stores only completed results. A PostgreSQL adapter should decide how callers observe a key left `IN_PROGRESS` after process death: wait, fail with retryable error, or allow takeover after an explicit timeout.

### `ReservationStore`

Required semantics:

- `reserve(account, amount)` must make the balance check and held-reservation creation one linearizable operation per account.
- `heldFor(accountId)` used by the reservation check must see all committed held reservations and must be protected from racing holds on the same account.
- `settle` and `release` are valid only from `HELD`.
- A reservation can resolve exactly once.
- Negative balances must not be hidden by clamping.

Recommended PostgreSQL pattern:

- Use one transaction for `ledger.balance(account)`, `heldFor(account)`, and `save(HELD)`.
- Protect that transaction with an account-scoped advisory lock or an explicit account balance/projection row lock.
- Add a version column or status transition constraint so two resolvers cannot both settle/release the same reservation.

Open design decision before implementation:

- The current service derives total balance by scanning the ledger. A production adapter should decide whether reservations lock on the account id alone or on a strongly consistent account balance projection.

### `WorkflowStore`

Required semantics:

- `save` durably records progress before a step starts and after it finishes.
- `load` must return the latest committed state for a workflow id.
- State data must be stable and serializable.
- Concurrent drivers for the same workflow must not corrupt or regress state.

Recommended PostgreSQL pattern:

- `workflow_states(workflow_id primary key, step_name, completed, data_json, version, updated_at)`.
- Use optimistic locking with a monotonically increasing version, or lock the workflow row while a driver claims it.
- Optionally keep a `workflow_history` append-only table for audit and debugging.

Open design decision before implementation:

- Decide whether the first JDBC adapter is only a state store or also a lease/claim mechanism for multi-worker execution.

### `RawWebhookStore`

Required semantics:

- Raw payload bytes and headers are persisted before any business processing.
- Signature verification must be performed against the exact raw bytes received.
- Duplicate provider event ids must be deterministic: either idempotently accepted or rejected as duplicates.
- Processing order must not be trusted.

Recommended PostgreSQL pattern:

- Unique key on `(provider, event_id)`.
- Columns for raw payload bytes, received timestamp, headers JSON, and verification metadata.
- Separate processing status if asynchronous workers claim webhooks from the table.

### `OutboxStore` and `ConsumerDeduplicationStore`

Required semantics:

- Outbox rows are written in the same transaction as the state change that produced them.
- Relays publish at least once. Consumers must deduplicate by stable event id.
- `markPublished` must not happen before a successful publish attempt.
- Relay workers must be able to divide pending work without double-claiming the same row indefinitely.

Recommended PostgreSQL pattern:

- `outbox_events(id primary key, topic, payload, metadata_json, created_at, published_at null, attempts, locked_until null)`.
- Relay query: select pending rows with `FOR UPDATE SKIP LOCKED` inside a short transaction, publish outside or with a lease depending on failure strategy, then mark published.
- `consumer_deduplication(event_id primary key, consumed_at)` with unique insert for `markIfFirst`.

### `AuditTrailStore`

Required semantics:

- Audit events are append-only.
- Events should include actor, reason, time, action, and metadata sufficient to explain the change.
- The store should support complete export for external audit review.
- If tamper evidence is enabled, hash-chain computation must happen in the same transaction as append.

Recommended PostgreSQL pattern:

- `audit_events(id primary key, actor, reason, action, occurred_at, metadata_json, previous_hash, hash)`.
- Restrict adapter API to insert and read operations. Operational deletion, if legally required, must happen through a separate controlled retention process, not normal application code.

## Serialization choices

Production tables should use explicit columns for values that participate in constraints, joins, or ordering. JSON is acceptable for metadata and provider-specific payloads, but not as the only source for core financial invariants.

Recommended storage forms:

| Value | Preferred durable form |
|---|---|
| Fiat/asset identity | asset code plus scale; crypto should include network and contract identity |
| Amount | decimal string, or mantissa + scale; never binary floating point |
| Timestamp | `timestamptz` for instants; explicit columns for booking/value/settlement semantics |
| Metadata | versioned JSONB with documented keys where used for behavior |
| Raw provider payload | byte array/text exactly as received, plus content type/encoding |
| Result replay payload | versioned JSON or durable reference id |

## Contract tests required before adapters

Before implementing JDBC/PostgreSQL adapters, add reusable contract tests that each adapter must pass:

- Ledger append is atomic and duplicate-safe.
- Ledger entries remain balanced after round-trip persistence, including numerically equal amounts with different `BigDecimal` scales.
- Idempotency collapses concurrent duplicate operations to one effect.
- Idempotency rejects or flags repeated keys with different payload hashes.
- Reservations cannot over-reserve under concurrent attempts.
- Reservation resolution is single-use.
- Workflow state survives a simulated crash at every save boundary and resumes to completion.
- Raw webhooks preserve exact payload bytes and deduplicate provider event ids.
- Outbox append participates in the same transaction as a representative state change.
- Outbox relay and consumer deduplication tolerate duplicate delivery.
- Audit append is insert-only and preserves event order metadata.

## Adapter implementation checklist

Do not start a PostgreSQL adapter until these items are answered for the target store:

- What is the primary key?
- What unique indexes encode idempotency or append-only behavior?
- Which method opens a transaction, and which method requires the caller to provide one?
- What isolation level or lock protects read-then-write decisions?
- What is the serialization format and version for every complex value?
- What happens on duplicate insert, lock timeout, deadlock, and serialization failure?
- Can the same contract test run against both the in-memory and JDBC implementation?

## Read next

- [Money representation](money-representation.md) — precise amount and asset storage choices
- [Ledger](ledger.md) — append-only double-entry accounting
- [Idempotency](idempotency.md) — duplicate request collapse
- [Workflows](workflows.md) — durable progress across crashes
- [Webhooks](webhooks.md) — raw external input persistence
- [Reconciliation](reconciliation.md) — detecting drift after storage or provider failures
