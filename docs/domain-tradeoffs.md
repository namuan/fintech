# Domain tradeoffs

## What problem this page solves

"Fintech" is not one domain. Payments, crypto custody, HFT, quant finance, consumer banking, and compliance‑heavy institutions have different constraints. A representation that is correct for a ledger is too slow for a market‑data feed. A representation that is fast enough for HFT is unsafe for accounting.

This page maps the major fintech sub‑domains to the library types and patterns that fit them best — and, just as importantly, warns when a type that works in one domain is dangerous in another.

## The library's answer: multiple types, explicit choices

The library does **not** pick one representation for you. It provides several, each with a clear domain and explicit tradeoffs:

| Type | Best for | Avoid for |
|---|---|---|
| `Money` + `DecimalAmount` | Ledgers, accounting, custody, payment settlement, tax reporting | High‑throughput market data, real‑time pricing |
| `Money` + `ScaledAmount` | High‑volume payment processing, wallet balances, interchange fees | Multi‑currency aggregation where scales differ |
| `ApproximateAmount` (quant) | Risk models, Monte Carlo, Greeks, pricing approximations | Account balances, settlement amounts, audit records |
| `MarketDataAmount` (trading) | Order books, tick data, execution prices — performance‑sensitive paths | Ledger postings (convert to `Money` before booking) |
| `Ledger` + `JournalEntry` | Core money state: who owns what, what moved when | Transient in‑flight state (use `WorkflowState`); query projections (build a `TrialBalance` cache) |
| `WorkflowState` + `WorkflowDriver` | Multi‑step money flows: withdrawals, deposits, FX conversions | Simple single‑step operations (use `IdempotencyBarrier` directly) |
| `ReconciliationJob` | Detecting drift between ledger/provider/bank/chain | Real‑time enforcement (use invariants and runtime checks) |

## Domain mapping

### 1. Consumer banking / custodial fintech

**Primary concern:** Exact balances, auditability, regulatory compliance.

**Recommended types:**
- `Money` with `DecimalAmount` or `ScaledAmount` for all balances.
- `Ledger` with double‑entry postings for every movement.
- `FinancialTimestampSet` with value time, booking time, and settlement time.
- `ReconciliationJob` comparing the ledger against each provider and bank statement.
- `MakerCheckerRequest` for manual corrections and large withdrawals.
- `PiiReference` to separate identity from financial records.

**Avoid:**
- `ApproximateAmount` or `double` for any balance or settlement amount.
- Skipping the reconciliation step; a missing webhook means a missing fact.

---

### 2. Payments / PSP integrations

**Primary concern:** Webhook reliability, authorisation vs capture, chargebacks, settlement batching.

**Recommended types:**
- `WebhookIngestionService` with `RawWebhookStore` and signature verification.
- `PaymentIntent` → `AuthorizationHold` → `Capture` → `Chargeback` flow types.
- `PspSettlementBatch` for one‑to‑many reconciliation.
- `OutboxRelay` for notifying downstream systems reliably.
- `IdempotencyBarrier` for every PSP API call.

**Avoid:**
- Trusting webhook content at face value; always confirm against the PSP API.
- Using the in‑memory stores in production; a restart loses pending outbox events.

---

### 3. Crypto wallets / exchanges / custody

**Primary concern:** Irreversible transactions, chain‑specific asset identity, confirmations, reorgs.

**Recommended types:**
- `CryptoAsset` with explicit `network` + `contractAddress` + `scale`.
- `ScaledAmount` with `BigInteger` mantissa — crypto precision often exceeds 64 bits.
- `ConfirmationPolicy` and `ReorgRiskPolicy` for finality decisions.
- `CryptoWithdrawalFlow` wiring reservation → compliance → broadcast → confirmation → ledger.
- `NetworkFeeEstimate` with reservation for the estimated fee; settle the actual fee afterward.

**Avoid:**
- Treating pegged/wrapped assets (USDC, WBTC) as equivalent to the underlying fiat or native coin.
- Assuming a single confirmation is final; wait for enough blocks and account for reorg risk.
- Broadcasting on‑chain without idempotency; a retry must re‑check the chain, not send twice.

---

### 4. FX and money conversion

**Primary concern:** Directional rates, spread, rate source, rounding.

**Recommended types:**
- `FxRate` with explicit `RateDirection` (from, to), `RateSource`, and `observedAt`.
- `FxQuote` with expiry — stale quotes must not be executed.
- `TransactionalConversion` producing the target amount given a quote and a `RoundingPolicy`.
- `ReferenceRate` for valuation (mark‑to‑market, tax basis); separate from the transactional rate.

**Avoid:**
- Inverting a rate to get the opposite direction (bid/ask spread makes this incorrect).
- Using a single "canonical" rate without tracking the source.
- Rounding implicitly; always apply an explicit `RoundingPolicy` and track residuals.

---

### 5. Trading / capital markets / HFT

**Primary concern:** Throughput, latency, tick sizes, order books.

**Recommended types:**
- `MarketDataAmount` — explicitly performance‑sensitive; marks values that should not be booked directly.
- `Order` / `Fill` / `Execution` / `Position` types for trade lifecycle.
- `TickSize` for price validation.
- `SettlementInstruction` for the boundary between trading and custody/settlement systems.

**Avoid:**
- Using `BigDecimal` in the hot path; convert to `Money` only when posting to the ledger.
- Confusing market‑data precision with accounting precision. They are different concerns.
- Skipping the explicit conversion boundary; every `MarketDataAmount` becomes a `Money` before it lands in the ledger.

---

### 6. Quant finance / risk modeling

**Primary concern:** Approximate numerical modelling, statistical accuracy, performance.

**Recommended types:**
- `ApproximateAmount` — explicitly marked as approximate, with an `ApproximationPolicy`.
- `RiskMetric` and `ValuationResult` for model outputs.
- `Tolerance` for comparing approximate results.

**Avoid:**
- Using `ApproximateAmount` for account balances or settlement amounts.
- Booking a model output directly into the ledger; convert through an explicit rounding and approval step.
- Pretending a Monte‑Carlo result is an exact value; tag it with the approximation policy.

---

### 7. Compliance / risk / operations

**Primary concern:** Audit trails, decision provenance, access controls, retention.

**Recommended types:**
- `AuditEvent` + `AuditTrailStore` for every sensitive action.
- `DecisionRecord` + `RuleEvaluationTrace` for compliance decisions (AML, KYC, sanctions).
- `MakerCheckerRequest` + `ApprovalPolicy` for sensitive manual actions.
- `Grant` + `AccessReview` for permission management.
- `PiiReference` + `ErasureRequestRecord` + `CryptoShreddingKeyRef` for data‑protection boundaries.

**Avoid:**
- Encoding jurisdiction‑specific rules (retention periods, AML thresholds) as library defaults.
- Skipping access reviews; stale permissions are a regulatory finding.

---

## Cross‑cutting guidance

### When to use ScaledAmount vs DecimalAmount

| Criterion | ScaledAmount | DecimalAmount |
|---|---|---|
| Storage format | `BigInteger` mantissa + `int` scale | `BigDecimal` |
| Arithmetic speed | Faster (integer math) | Slower (arbitrary‑precision decimal) |
| Cross‑asset aggregation | Error‑prone if scales differ | `BigDecimal` handles scale automatically |
| Best for | High‑volume single‑asset processing, crypto, HFT | Ledgers, accounting, multi‑currency aggregation |

### When to use the Ledger directly vs a Workflow

| Criterion | Ledger.post() | WorkflowDriver.run() |
|---|---|---|
| Operation complexity | Single atomic posting (one JournalEntry) | Multi‑step flow spanning time and external calls |
| Crash safety | One write; if it fails, retry with idempotency | Multiple steps; state persisted between each |
| External calls | None (the ledger is internal) | Yes — compliance checks, PSP calls, chain broadcasts |
| Rollback | Not needed (atomic at the store level) | Must compensate completed external steps |

### When to use reconciliation vs runtime invariants

| Criterion | ReconciliationJob | Runtime invariant check |
|---|---|---|
| When it runs | Periodically (hourly, daily, monthly) | At the moment of the operation |
| What it catches | Drift that has already happened | Violations before they are persisted |
| Cross‑system | Yes — ledger vs provider vs bank | No — within a single system / aggregate |
| Performance cost | None on the hot path | Added latency on every operation |

Both are needed. Invariants catch problems early; reconciliation catches what invariants miss.

## Read next

- [Money representation](money-representation.md) — the types referenced throughout this page
- [Ledger](ledger.md) — when to use the double‑entry ledger
- [Workflows](workflows.md) — when a single post is not enough
- [Reconciliation](reconciliation.md) — the safety net across systems
- [Principles](principles.md) — why these tradeoffs exist
