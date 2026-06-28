# Reconciliation

## What problem this package solves

Any system that relies on external data is prone to data drift — the ledger says one thing, the payment provider another, the bank statement a third. A missing webhook, a provider bug, or a delayed settlement can all cause silent mismatches. Without a systematic reconciliation process, those mismatches accumulate and erode trust in the books.

The `reconciliation` package provides a `ReconciliationJob` that compares two independent `ReconciliationSource` implementations and surfaces every break. It supports exact‑id matching, heuristic (amount + time) matching, and one‑to‑many settlement matching.

## Package layout

```
com.github.namuan.fintech.reconciliation
  ReconciliationRecord   – (id, source, amount, effectiveAt, attributes) — one fact from one system
  ReconciliationSource   – @FunctionalInterface: produce a list of records to compare
  MatchRule              – @FunctionalInterface: does a left record match a right record?
  MatchResult            – (left, right, matched, reason) — result of comparing a pair
  ReconciliationBreak    – (id, record, description) — a single unmatched record
  ReconciliationResolution – (breakId, action, explanation) — how a break was resolved
  ReconciliationJob      – runs a MatchRule against two sources and returns all breaks
  HeuristicMatcher       – matches by amount + asset + time tolerance (when ids differ)
  OneToManyMatcher       – checks if a single settlement record equals the sum of many transaction records
```

## What it deliberately does not solve

- The reconciliation job does **not** understand settlement timing. If a provider settles at T+3, records that are simply not yet settled will appear as breaks. Callers must filter those expected‑unreconciled records by effective date before reporting.
- It does **not** auto‑resolve breaks. Every `ReconciliationBreak` must be understood and resolved through a first‑class action — a ledger correction, a webhook replay, or a provider dispute. Blindly overwriting data to make reconciliation pass is forbidden by design.
- It does **not** schedule itself. The `ReconciliationJob` is a plain method call. Wire it into a cron, a scheduled executor, or a workflow step.
- It does **not** handle fuzzy matching beyond simple heuristics. For complex multi‑field fuzzy matching, compose a custom `MatchRule` or integrate an external matching engine.

## Safe defaults

- `MatchRule.sameIdAndAmount()` is the simplest, safest matcher: records match when their ids and amounts are identical.
- `HeuristicMatcher` requires both amount *and* time tolerance to match — it will not match records with different amounts.
- `OneToManyMatcher` validates that all "many" records share the same asset as the "one" record before summing.
- Breaks are returned as a list; the caller decides which ones are expected (settlement delay) vs alarming (genuine data loss).

## Dangerous tradeoffs

| Tradeoff | Risk | Mitigation |
|---|---|---|
| Using heuristic matching without an id | Two different transactions of the same amount/time look like a match | Always persist the external provider’s id in your system so exact matching is possible |
| Filtering out "expected" breaks too aggressively | A genuine problem hidden behind a settlement‑delay filter is never investigated | Report both "expected unreconciled" and "unexpected breaks" separately |
| Reconciling only amounts, not assets | A EUR record matched against a USD record of the same numeric value | The `HeuristicMatcher` checks asset equality; all matchers should |
| Reconciling too infrequently | A problem festers for a month; correction becomes expensive and complex | Match cadence to business criticality — daily for payments, hourly for custody, real‑time for trading |

## Example usage

```java
// Source A: internal ledger records
ReconciliationSource ledgerSource = () -> ledgerStore.all().stream()
    .flatMap(e -> e.postings().stream())
    .map(p -> new ReconciliationRecord(
        e.id().value(), "ledger", p.amount(), e.timestamps().valueTime(), Map.of()
    )).toList();

// Source B: provider records (from persisted API responses)
ReconciliationSource providerSource = () -> providerRecordStore.all().stream()
    .map(r -> new ReconciliationRecord(
        r.id(), "provider", r.amount(), r.effectiveAt(), Map.of()
    )).toList();

// Run reconciliation
ReconciliationJob job = new ReconciliationJob(MatchRule.sameIdAndAmount());
List<ReconciliationBreak> breaks = job.compare(ledgerSource, providerSource);

for (var b : breaks) {
    if (isSettlementDelay(b)) {
        log.info("Expected: {}", b.description());
    } else {
        alertingService.alert("Reconciliation break: " + b);
        // Investigate, then resolve with:
        //   - a ledger correction entry
        //   - a webhook replay
        //   - a provider support ticket
    }
}
```

## Read next

- [Webhooks](webhooks.md) — reconciliation is the safety net that catches the dropped webhook
- [Ledger](ledger.md) — the internal source of truth that reconciliation verifies
- [Principles](principles.md) — the "no trust" principle and why you compare independent sources
