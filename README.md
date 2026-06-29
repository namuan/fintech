# Fintech Engineering Library

A monolithic Java library providing reusable primitives, workflows, audit infrastructure, integration patterns, and testing support for software systems that handle money — across payments, banking, crypto, FX, trading, compliance, reconciliation, and distributed workflow domains.

Built from the patterns described in the [Fintech Engineering Handbook](https://w.pitula.me/fintech-engineering-handbook/).

## Quick start

**Requirements:** Java 21+, Maven

```xml
<dependency>
    <groupId>com.github.namuan</groupId>
    <artifactId>fintech</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

```java
import com.github.namuan.fintech.currency.FiatCurrency;
import com.github.namuan.fintech.money.Money;
import com.github.namuan.fintech.ledger.*;
import com.github.namuan.fintech.time.FinancialTimestampSet;
import java.time.Instant;
import java.util.List;
import java.util.Map;

// 1. Define money
FiatCurrency usd = new FiatCurrency("USD", 2, "US Dollar");
Money deposit = Money.decimal("100.00", usd);

// 2. Set up accounts
Account cash = new Account(new AccountId("cash"), "Cash", AccountType.ASSET, usd);
Account user = new Account(new AccountId("user"), "User Balance", AccountType.LIABILITY, usd);

// 3. Post a balanced journal entry
Ledger ledger = new Ledger(new InMemoryLedgerStore());
ledger.post(new JournalEntry(
    JournalEntryId.random(),
    List.of(
        new Posting(cash.id(), DebitCredit.DEBIT,  deposit, "incoming"),
        new Posting(user.id(), DebitCredit.CREDIT, deposit, "user balance")
    ),
    FinancialTimestampSet.bookedNow(Instant.now()),
    "card deposit",
    Map.of("provider", "stripe")
));

// 4. Derive balance — never stored, always computed
System.out.println(ledger.balance(user).decimalValue()); // 100.00
```

## Design principles

1. **No invented data** — prevent accidental money creation through duplicate execution, bad rounding, or cross‑currency arithmetic.
2. **No lost data** — preserve precision, raw inputs, audit trails, and workflow progress.
3. **No trust** — treat external providers, webhooks, internal callers, and cached state as untrusted until verified.
4. **Explicit over implicit** — currency, scale, rounding, rate source, timestamp semantics, and idempotency scope must be explicit.
5. **Domain‑specific escape hatches** — payments, crypto, HFT, quant, banking, and compliance need different choices. The library makes those choices visible.

## Package map

| Package | Purpose |
|---|---|
| `money` | `Money`, `Amount` (sealed: `ScaledAmount`, `DecimalAmount`), parser, formatter |
| `currency` | `AssetId` (sealed: `FiatCurrency`, `CryptoAsset`), `CurrencyUnit` |
| `rounding` | `RoundingPolicy`, `RoundingResult`, `Residual`, `RoundingModeRegistry` |
| `serialization` | `MoneyJson` — JSON‑safe string‑amount representation |
| `fx` | `FxRate`, `FxQuote`, `TransactionalConversion`, `ReferenceRate`, `SpreadBooking` |
| `time` | `FinancialTimestampSet`, `ValueTime`, `BookingTime`, `SettlementTime`, `ReportingPeriod` |
| `ledger` | `JournalEntry`, `Posting`, `Account`, `Ledger`, `TrialBalance`, `ReversalEntry`, `CorrectionEntry` |
| `audit` | `AuditEvent`, `AuditTrailStore`, `DecisionRecord`, `ComplianceDecision`, `ManualReviewRecord` |
| `idempotency` | `IdempotencyBarrier`, `IdempotencyKey`, `IdempotencyScope`, `IdempotentResult` |
| `reservations` | `ReservationService`, `Reservation`, `AvailableBalanceCalculator` |
| `workflow` | `WorkflowDriver`, `MoneyWorkflow`, `WorkflowState`, `Saga`, `CompensationAction` |
| `integration` | `ExternalProvider`, `ProviderRequestRecord`, `ProviderResponseRecord`, `BoundaryValidator` |
| `webhooks` | `WebhookIngestionService`, `RawWebhookStore`, `WebhookSignatureVerifier`, `WebhookDeduplicator` |
| `outbox` | `OutboxRelay`, `OutboxStore`, `PublishedEventId`, `ConsumerDeduplicationStore` |
| `reconciliation` | `ReconciliationJob`, `MatchRule`, `HeuristicMatcher`, `OneToManyMatcher`, `ReconciliationBreak` |
| `lineage` | `LineageRecord`, `ReplayContext`, `InputVersion`, `ContentAddressedBlobRef` |
| `controls` | `MakerCheckerRequest`, `ApprovalPolicy`, `Role`, `Grant`, `AccessReview`, `BreakGlassOverride` |
| `compliance` | `PiiReference`, `ErasureRequestRecord`, `CryptoShreddingKeyRef`, `JurisdictionContext` |
| `payments` | `PaymentIntent`, `AuthorizationHold`, `Capture`, `Chargeback`, `PspSettlementBatch` |
| `banking` | `BankTransferInstruction`, `BalanceCheckObservation`, `BankSettlementEvent`, `AchDebitFlow` |
| `crypto` | `CryptoWithdrawalFlow`, `OnChainTransaction`, `ConfirmationPolicy`, `ReorgRiskPolicy`, `NetworkFeeEstimate` |
| `trading` | `Order`, `Fill`, `Execution`, `Position`, `Instrument`, `TickSize`, `MarketDataAmount` |
| `quant` | `ApproximateAmount`, `ApproximationPolicy`, `RiskMetric`, `ValuationResult`, `Tolerance` |

## Non‑goals

- This library is **not legal, regulatory, tax, or accounting advice.**
- It does **not** encode jurisdiction‑specific compliance rules as universal defaults.
- It is **not** a full core‑banking system, PSP, exchange, AML engine, or ledger product.
- It does **not** hide domain‑specific tradeoffs behind magical one‑size‑fits‑all abstractions.
- It does **not** claim exactly‑once distributed delivery; it supports effectively‑once processing through idempotency, durable state, and reconciliation.

## Documentation

Start with the [documentation index](docs/index.md), or jump directly to a topic:

| Topic | Description |
|---|---|
| [Principles](docs/principles.md) | The three principles and how they appear throughout the library |
| [Money representation](docs/money-representation.md) | How amounts, currencies, and crypto assets are modelled |
| [Ledger](docs/ledger.md) | Double‑entry bookkeeping, postings, corrections, and reversals |
| [Idempotency](docs/idempotency.md) | Surviving retries without double‑counting |
| [Workflows](docs/workflows.md) | Durable multi‑step money flows |
| [Storage semantics](docs/storage-semantics.md) | Durable-store contracts and PostgreSQL adapter requirements |
| [Webhooks](docs/webhooks.md) | Safely ingesting signals from the outside world |
| [Reconciliation](docs/reconciliation.md) | Detecting and resolving data drift across systems |
| [Compliance boundaries](docs/compliance-boundaries.md) | PII separation, retention, and jurisdiction awareness |
| [Domain tradeoffs](docs/domain-tradeoffs.md) | Why payments, crypto, HFT, and quant need different choices |

## Build and test

```bash
# Run all tests
mvn test

# Compile only
mvn compile
```

## Implementation status

The library implements the first‑pass skeleton of all planned packages. Stores are in‑memory (`InMemoryLedgerStore`, `InMemoryIdempotencyStore`, etc.) suitable for tests and single‑node services. JDBC/PostgreSQL adapters and deeper property‑based tests are planned as next steps.

See [TODO.md](TODO.md) for the full status.

## Acknowledgements

Inspired by the [Fintech Engineering Handbook](https://w.pitula.me/fintech-engineering-handbook/) by Voytek Pitula.

## License

[MIT](LICENSE)
