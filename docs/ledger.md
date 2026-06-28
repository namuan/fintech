# Ledger

## What problem this package solves

Money movements must be recorded in a way that always balances, survives audit, and can be reconstructed years later. The `ledger` package implements double‑entry bookkeeping with immutable postings, corrections through compensating entries, and in‑memory stores suitable for testing and single‑service deployments.

## Package layout

```
com.github.namuan.fintech.ledger
  AccountId           – value‑object identifier for an account
  Account             – (id, name, type, asset) — the subject of postings
  AccountType         – ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
  DebitCredit         – DEBIT, CREDIT — the two sides of every posting
  Posting             – (accountId, side, amount, memo) — one leg of a journal entry
  JournalEntryId      – value‑object identifier for a journal entry
  JournalEntry        – ordered list of Postings + timestamps + reason + metadata
  LedgerInvariantChecker – validates debits == credits per asset
  ChartOfAccounts     – registry of known accounts
  LedgerStore         – append‑only store interface
  InMemoryLedgerStore – thread‑safe ConcurrentHashMap implementation
  Ledger              – posts entries and derives balances from the store
  TrialBalance        – snapshot of account balances
  CorrectionLink      – (original entry, correction entry, REVERSAL or ADJUSTMENT)
  CorrectionType      – REVERSAL, ADJUSTMENT
  ReversalEntry       – constructs a full economic reversal of an existing entry
  CorrectionPolicy    – ALLOW_OPEN_PERIOD_ONLY or ALLOW_WITH_APPROVAL
  CorrectionEntry     – bundles a JournalEntry with its CorrectionLink
  CompensatingEntry   – bundles a JournalEntry with a human explanation
```

## What it deliberately does not solve

- The ledger is **not an ACID transaction manager**. It calls `LedgerStore.append` and trusts the store implementation to enforce idempotency and consistency. The JDBC adapter (future work) will use `INSERT` with uniqueness constraints; the in‑memory store uses `putIfAbsent`.
- It does **not** implement the full accounting equation (`assets = liabilities + equity + revenue − expenses`) as an automatic check. `LedgerInvariantChecker` validates that each *journal entry* balances, but cross‑account equation checks are the caller’s responsibility.
- It does **not** cache balances. `Ledger.balance(account)` scans every entry every time. For production use, derive a cache or projection from the event stream.
- It does **not** handle multi‑book / multi‑entity setups. The `LedgerStore` is a single logical book. Compose multiple stores for separate legal entities or reporting standards.

## Safe defaults

- `JournalEntry` refuses fewer than two postings.
- `LedgerInvariantChecker.requireBalanced` throws if debits ≠ credits per asset.
- `InMemoryLedgerStore.append` uses `putIfAbsent` — a journal entry id can only be posted once.
- `Posting` requires a non‑negative amount; the sign convention is carried by the `DebitCredit` side.
- `ReversalEntry.reverse` creates a mirror entry with flipped sides, preserving the link back to the original.

## Dangerous tradeoffs

| Tradeoff | Risk | Mitigation |
|---|---|---|
| Deriving balance by scanning all entries | O(n) per call, unusable at scale | Project to a `TrialBalance` cache; rebuild from the event stream after a restart |
| Allowing arbitrary `AccountId` values | Typo in an id means money lands on the wrong account | Validate through `ChartOfAccounts` before posting; use type‑safe account construction |
| Using `InMemoryLedgerStore` in production | Power loss = total data loss | This is a test/development store; use a JDBC adapter for production |
| Backdating into a closed period | Reported numbers change after the fact | Enforce `CorrectionPolicy.ALLOW_OPEN_PERIOD_ONLY` and close periods explicitly |

## Example usage

```java
// Set up accounts
Account cash = new Account(AccountId.random(), "Cash", AccountType.ASSET, usd);
Account user = new Account(AccountId.random(), "User Deposits", AccountType.LIABILITY, usd);
Account fees  = new Account(AccountId.random(), "Fee Revenue", AccountType.REVENUE, usd);

Ledger ledger = new Ledger(new InMemoryLedgerStore());

// A deposit: cash debited, user credited
ledger.post(new JournalEntry(
    JournalEntryId.random(),
    List.of(
        new Posting(cash.id(), DebitCredit.DEBIT,  Money.decimal("100.00", usd), "incoming"),
        new Posting(user.id(), DebitCredit.CREDIT, Money.decimal("100.00", usd), "user balance")
    ),
    FinancialTimestampSet.bookedNow(Instant.now()),
    "card deposit",
    Map.of("providerId", "psp-42")
));

// A fee: user debited, revenue credited
ledger.post(new JournalEntry(
    JournalEntryId.random(),
    List.of(
        new Posting(user.id(),  DebitCredit.DEBIT,  Money.decimal("1.50", usd), "monthly fee"),
        new Posting(fees.id(),  DebitCredit.CREDIT, Money.decimal("1.50", usd), "fee income")
    ),
    FinancialTimestampSet.bookedNow(Instant.now()),
    "monthly maintenance fee",
    Map.of()
));

// Balances are derived, not stored
System.out.println(ledger.balance(user)); // 98.50
```

## Read next

- [Principles](principles.md) — the "no invented data" principle and why entries must balance
- [Idempotency](idempotency.md) — ensuring a retried post creates exactly one entry
- [Reconciliation](reconciliation.md) — verifying the ledger against external sources
