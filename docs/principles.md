# Principles

The fintech library is built on three foundational principles taken from the Fintech Engineering Handbook. Every package, every type, and every default should trace back to at least one of these.

## 1. No invented data

Money cannot be created out of nowhere.

**What this means in the library**

- `Money.plus` and `Money.minus` forbid cross-currency arithmetic. Adding EUR to USD is a compile‑time or runtime error.
- `JournalEntry` construction rejects unbalanced postings. You cannot commit an entry where debits ≠ credits.
- `IdempotencyBarrier` ensures a retried operation produces exactly one effect, not two. A duplicate withdrawal does not debit twice.
- `RoundingPolicy` always tracks residuals. When `1.00` is split three ways and rounded, the leftover fraction is not silently dropped.
- `ReversalEntry` fixes an error by posting a compensating entry, never by mutating the original. The books stay balanced.

**What it does *not* solve**

- It does not prevent an application from calling `Money.decimal("123.00", usd)` when only `100.00` is available. The invariant checker catches unbalanced entries, but business rules (available balance, overdraft limits) are domain‑specific.
- It does not stop an operator from posting a wrong manual adjustment. The correction/ reversal machinery makes that fix traceable, not impossible.

## 2. No lost data

Everything that happens to money must be tracked and persisted.

**What this means in the library**

- `Amount` supports arbitrary precision through `DecimalAmount` (backed by `BigDecimal`) and `ScaledAmount` (backed by `BigInteger` + scale). Binary floating‑point is never the default for accounting values.
- `FinancialTimestampSet` carries `valueTime`, `bookingTime`, and optional `settlementTime` — three distinct instants instead of a single `createdAt`.
- `AuditEvent` captures `what`, `when`, `who`, and `why`. The audit trail is append‑only by design.
- `RawWebhookStore` persists the raw bytes of every incoming webhook before any business logic runs. If processing crashes, the payload is still there.
- `WorkflowDriver` persists workflow state (`WorkflowState`) before and after every step. A crash between steps leaves enough information to resume.
- `OutboxStore` commits the publish intent transactionally with the state change so that a committed change is never silently lost to downstream consumers.
- `LineageRecord` and `ReplayContext` record what input versions were used so past computations can be reproduced.

**What it does *not* solve**

- The library does not automatically archive old audit events or manage storage costs. That is an operational concern.
- It does not decide which events are worth persisting. Callers choose what to write.

## 3. No trust

Trust neither external providers, internal components, nor the world.

**What this means in the library**

- `WebhookIngestionService` verifies the caller’s signature before storing the payload, and the recommended `WebhookAsHintPolicy` is `REQUIRE_AUTHORITATIVE_API_CONFIRMATION`.
- `BoundaryValidator<T>` lets you validate external API responses at the boundary so malformed data does not leak into core domain logic.
- `ReconciliationJob` compares two independent `ReconciliationSource` implementations — ledger vs provider, bank vs chain, internal projection vs custodian report — and surfaces every break.
- `IdempotencyBarrier` can be configured with `RepeatedPayloadPolicy.REQUIRE_SAME_HASH` so that a caller changing the payload on retry is detected.
- `BalanceCheckObservation` is explicitly non‑authoritative by default. An ACH balance check is a hint, not a guarantee.
- `ProviderRedundancyPolicy` models the minimum number of independent sources that must agree before a fact is accepted.

**What it does *not* solve**

- The library does not implement HMAC verification, TLS, or mTLS. It provides the hooks (`WebhookSignatureVerifier`) but the actual crypto is the caller’s responsibility.
- It does not decide *which* reconciliation disagreements are acceptable. It surfaces every break; the caller decides whether to alert or auto‑resolve.

## How the principles show up in tests

The test suite (`CoreLibraryTest` and `RemainingPackagesTest`) validates:

| Principle | Test |
|---|---|
| No invented data | Cross‑currency arithmetic throws, unbalanced journal entries throw, idempotency collapses duplicates, rounding residuals are tracked |
| No lost data | Workflow state persists across steps, webhook payloads are stored, timestamps are three‑part |
| No trust | Reconciliation catches missing records, maker‑checker enforces approval, webhook signature validation is required |

## Read next

- [Money representation](money-representation.md) — how amounts and currencies are modelled
- [Ledger](ledger.md) — double‑entry bookkeeping, postings, and corrections
- [Idempotency](idempotency.md) — surviving retries without double‑counting
- [Workflows](workflows.md) — durable multi‑step money flows
- [Webhooks](webhooks.md) — safely ingesting signals from the outside world
- [Reconciliation](reconciliation.md) — detecting and resolving data drift
- [Compliance boundaries](compliance-boundaries.md) — PII separation, retention, and jurisdiction awareness
- [Domain tradeoffs](domain-tradeoffs.md) — why payments, crypto, HFT, and quant need different choices
