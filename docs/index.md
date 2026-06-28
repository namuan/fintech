# Documentation index

Welcome to the Fintech Engineering Library documentation.

## Reading order

If you are new to financial software engineering, read in this order:

1. **[Principles](principles.md)** — the three rules that shape every decision: no invented data, no lost data, no trust.
2. **[Money representation](money-representation.md)** — how amounts, currencies, and crypto assets are modelled, stored, and serialised.
3. **[Ledger](ledger.md)** — double‑entry bookkeeping: how money moves between accounts and how corrections work.
4. **[Idempotency](idempotency.md)** — why every operation that touches money must survive retries.
5. **[Workflows](workflows.md)** — durable multi‑step money flows that survive crashes between steps.
6. **[Webhooks](webhooks.md)** — how to safely ingest signals from payment providers, banks, and custodians.
7. **[Reconciliation](reconciliation.md)** — the safety net that catches data drift between systems.
8. **[Compliance boundaries](compliance-boundaries.md)** — separating PII from financial data, retention, access controls, and maker‑checker.
9. **[Domain tradeoffs](domain-tradeoffs.md)** — a guide to choosing the right types and patterns for banking, payments, crypto, FX, trading, HFT, quant, and compliance.

If you are experienced in one fintech domain and want to understand how the library handles a different one, start with **[Domain tradeoffs](domain-tradeoffs.md)**.

## By topic

### Core primitives

| Page | What it covers |
|---|---|
| [Principles](principles.md) | No invented data, no lost data, no trust — how they show up in every package |
| [Money representation](money-representation.md) | `Money`, `Amount`, `ScaledAmount`, `DecimalAmount`, `FiatCurrency`, `CryptoAsset`, serialisation, forbidden cross‑currency arithmetic |

### Recording and moving money

| Page | What it covers |
|---|---|
| [Ledger](ledger.md) | `JournalEntry`, `Posting`, `Account`, `Ledger`, `TrialBalance`, `ReversalEntry`, `CorrectionEntry`, clearing accounts |
| [Workflows](workflows.md) | `WorkflowDriver`, `MoneyWorkflow`, `WorkflowState`, `Saga`, `CompensationAction`, durable execution patterns |

### Safety patterns

| Page | What it covers |
|---|---|
| [Idempotency](idempotency.md) | `IdempotencyBarrier`, keys, scopes, replay semantics, payload validation |
| [Webhooks](webhooks.md) | Raw payload persistence, signature verification, deduplication, hint‑based processing |
| [Reconciliation](reconciliation.md) | Comparing ledger vs provider vs bank vs chain, exact and heuristic matching, one‑to‑many settlement matching |

### Controls and compliance

| Page | What it covers |
|---|---|
| [Compliance boundaries](compliance-boundaries.md) | PII separation, `ErasureRequestRecord`, `CryptoShreddingKeyRef`, `MakerCheckerRequest`, `ApprovalPolicy`, `BreakGlassOverride`, `JurisdictionContext` |

### Choosing the right approach

| Page | What it covers |
|---|---|
| [Domain tradeoffs](domain-tradeoffs.md) | When to use `ScaledAmount` vs `DecimalAmount` vs `ApproximateAmount`; when to use `Ledger` vs `Workflow` vs `Reconciliation`; domain‑specific recommendations for banking, payments, crypto, FX, trading, HFT, quant, and compliance |

## Package reference

For a complete list of packages and types, see the [package map in the README](../README.md#package-map).

## Source files

- [TODO.md](../TODO.md) — implementation status and remaining work
- [Fintech Engineering Handbook](https://w.pitula.me/fintech-engineering-handbook/) — the handbook that inspired this library
