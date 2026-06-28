# Java Fintech Monolith Library Plan

## Goal

Create a single Java library that provides reusable primitives, workflows, audit infrastructure, integration patterns, and testing support for software systems that handle money across payments, banking, crypto, FX, trading, compliance, reconciliation, and distributed workflow domains.

The library should be a **monolith artifact** from the user's point of view, but internally organized into clear packages so teams can adopt only the parts they need.

## Non-goals

- This library is not legal, regulatory, tax, or accounting advice.
- It should not encode jurisdiction-specific compliance rules as universal defaults.
- It should not try to be a full core banking system, PSP, exchange, AML engine, or ledger product.
- It should not hide domain-specific tradeoffs behind magical one-size-fits-all abstractions.
- It should not claim exactly-once distributed delivery; it should support effectively-once processing through idempotency, durable state, and reconciliation.

## Design principles

1. **No invented data**
   - Prevent accidental money creation through duplicate execution, bad rounding, unsafe balance mutation, or cross-currency arithmetic.

2. **No lost data**
   - Preserve precision, raw inputs, audit trails, old event formats, workflow progress, and reconciliation evidence.

3. **No trust**
   - Treat external providers, webhooks, internal callers, operators, and even cached state as untrusted until verified.

4. **Explicit over implicit**
   - Currency, scale, rounding, rate source, timestamp semantics, idempotency scope, and provider identity must be explicit.

5. **Domain-specific escape hatches**
   - Payments, crypto, HFT, quant modeling, banking, and compliance need different choices. The library should make those choices visible rather than pretending one representation fits all.

6. **Append-only by default**
   - Financial records, audit records, workflow history, and corrections should be modeled as append-only facts.

## Proposed Java baseline

- Java version: **Java 21+**
- Build: Maven or Gradle; decide in implementation phase.
- Package root: `com.example.fintech` initially, replace with final group ID later.
- Artifact shape: one primary jar, with optional integrations activated by adapters/configuration.
- Dependencies: keep core dependency-light; isolate heavy integrations behind adapter packages.

## High-level package layout

```text
com.example.fintech
  money
  currency
  rounding
  fx
  ledger
  accounts
  time
  audit
  events
  idempotency
  workflow
  reservations
  overdraft
  integration
  webhooks
  outbox
  reconciliation
  controls
  compliance
  crypto
  trading
  testing
  storage
  serialization
  observability
```

## Phase 1: Foundation primitives

### 1. Money and amount model

Deliverables:

- `Money`
- `CurrencyUnit`
- `AssetId`
- `FiatCurrency`
- `CryptoAsset`
- `Amount`
- `ScaledAmount`
- `DecimalAmount`
- `MoneyParser`
- `MoneyFormatter`

Design requirements:

- Forbid arithmetic between different currencies/assets.
- Support fiat ISO 4217 metadata.
- Support crypto identity by network + contract address + decimals.
- Support integer minor-unit storage.
- Support arbitrary-precision decimal calculation.
- Support mantissa + scale representation for APIs.
- Avoid binary floating point in core accounting primitives.
- Allow explicit domain adapters for quant/risk use cases where `double` is acceptable.

Key decisions to document:

- Difference between storage representation and computation representation.
- Difference between internal model and API/interchange model.
- Canonical JSON representation options:
  - string decimal amount + currency
  - integer amount + scale + currency
  - mantissa + exponent + currency

### 2. Rounding

Deliverables:

- `RoundingPolicy`
- `RoundingContext`
- `RoundingModeRegistry`
- `RoundingResult`
- `Residual`
- `RoundingAccountPolicy`

Design requirements:

- Rounding must always require an explicit policy.
- Track residuals when a rounded split no longer sums to original.
- Support legal/business labels on policies.
- Support golden-test fixtures for rounding-sensitive calculations.

### 3. Time model

Deliverables:

- `FinancialTimestampSet`
- `ValueTime`
- `BookingTime`
- `SettlementTime`
- `EffectiveDate`
- `ReportingPeriod`
- `ClosedPeriodPolicy`

Design requirements:

- Avoid generic `createdAt` as the only time field.
- Support backdated and forward-dated transactions.
- Support settlement delays such as `T+2`.
- Prevent or flag corrections into closed periods according to policy.

## Phase 2: Ledger and accounting core

### 1. Double-entry ledger

Deliverables:

- `Ledger`
- `JournalEntry`
- `Posting`
- `DebitCredit`
- `Account`
- `AccountId`
- `AccountType`
- `ChartOfAccounts`
- `TrialBalance`
- `LedgerInvariantChecker`

Design requirements:

- Every movement has source and destination semantics.
- Entries balance by construction where possible.
- Account types support assets, liabilities, equity, revenue, and expenses.
- Posted entries are immutable.
- Balances are derived from postings, with optional projections/caches.
- External providers, PSPs, banks, chains, and custodians can be modeled as accounts.

### 2. Corrections and reversals

Deliverables:

- `ReversalEntry`
- `CorrectionEntry`
- `CompensatingEntry`
- `CorrectionLink`
- `CorrectionPolicy`

Design requirements:

- Never mutate posted entries.
- Link original and correction both ways.
- Distinguish economic reversal from adjustment.
- Respect closed-period policy.

### 3. Clearing, suspense, and settlement support

Deliverables:

- `ClearingAccount`
- `SuspenseAccount`
- `SettlementBatch`
- `NettingGroup`
- `ReceivablePayableModel`

Design requirements:

- Model money in transit.
- Support one settlement covering many transactions.
- Support netting and batch reconciliation.

## Phase 3: Execution safety

### 1. Idempotency

Deliverables:

- `IdempotencyKey`
- `IdempotencyScope`
- `IdempotencyStore`
- `IdempotencyBarrier`
- `IdempotentResult`
- `RepeatedPayloadPolicy`
- `IdempotencyTestHarness`

Design requirements:

- Explicit keys preferred.
- Keys scoped by operation, actor/client, and domain.
- Atomic barrier for concurrent duplicate requests.
- Configurable replay behavior for stored successes and failures.
- Optional repeated-payload validation.
- No default short time window unless caller explicitly accepts correctness tradeoff.

### 2. Funds reservation

Deliverables:

- `Reservation`
- `ReservationId`
- `ReservationService`
- `AvailableBalanceCalculator`
- `ReservationExpiryPolicy`
- `ReservationSettlement`
- `ReservationRelease`

Design requirements:

- Available balance = total balance - reserved funds.
- Balance check and reservation must be linearizable in adapter implementation.
- Every reservation must settle, release, or expire.
- Support estimated amount vs actual settlement amount.

### 3. Overdraft handling

Deliverables:

- `OverdraftEvent`
- `OverdraftPolicy`
- `IntentionalOverdraftFacility`
- `UnintentionalOverdraftDetector`
- `RecoveryPlan`

Design requirements:

- Negative balances must be representable.
- Do not clamp to zero.
- Detect and investigate policy violations.
- Support recovery by future deposits, repayment, or write-off.

### 4. Durable workflows

Deliverables:

- `MoneyWorkflow`
- `WorkflowState`
- `WorkflowStep`
- `WorkflowStore`
- `WorkflowDriver`
- `RetryPolicy`
- `CompensationAction`
- `Saga`

Design requirements:

- Persist progress before moving to next step.
- Resume stalled workflows through an independent driver.
- Every step must be safe to rerun.
- External side effects roll forward or compensate.
- Provide adapter interfaces for Temporal/Camunda/Step Functions later.

## Phase 4: External world integration

### 1. API client safety layer

Deliverables:

- `ExternalProvider`
- `ProviderRequestRecord`
- `ProviderResponseRecord`
- `BoundaryValidator`
- `ProviderQuotaPolicy`
- `ProviderRedundancyPolicy`
- `ProviderSandboxWarning`

Design requirements:

- Persist structured request and response records.
- Validate only required fields at boundaries.
- Fail loudly on critical malformed data.
- Support provider-specific quirks without contaminating core models.

### 2. Webhook ingestion

Deliverables:

- `WebhookEnvelope`
- `RawWebhookStore`
- `WebhookSignatureVerifier`
- `WebhookDeduplicator`
- `WebhookProcessor`
- `WebhookAsHintPolicy`

Design requirements:

- Verify signature over raw bytes.
- Persist raw payload before processing.
- Acknowledge fast after durable storage.
- Process asynchronously.
- Do not trust ordering, delivery, uniqueness, or payload truth.
- Encourage querying authoritative API state.

### 3. Reliable notification

Deliverables:

- `OutboxEvent`
- `OutboxStore`
- `OutboxRelay`
- `CdcEventAdapter`
- `PublishedEventId`
- `ConsumerDeduplicationStore`

Design requirements:

- State change and publish intent written transactionally.
- Delivery is at-least-once.
- Consumers deduplicate on stable event ID.
- CDC adapters should avoid leaking internal table schemas.

## Phase 5: Reconciliation and data lineage

### 1. Reconciliation engine

Deliverables:

- `ReconciliationJob`
- `ReconciliationSource`
- `ReconciliationRecord`
- `MatchRule`
- `MatchResult`
- `ReconciliationBreak`
- `ReconciliationResolution`
- `OneToManyMatcher`
- `HeuristicMatcher`

Design requirements:

- Compare ledger, provider, bank, chain, custodian, and internal projections.
- Support missing records and mismatched records.
- Account for settlement delays and expected unreconciled windows.
- Never blindly overwrite data to make reconciliation pass.
- Resolution happens through first-class correction or reprocessing.

### 2. Data lineage

Deliverables:

- `LineageRecord`
- `SourceSnapshotId`
- `InputVersion`
- `TransformationStep`
- `ReplayContext`
- `ContentAddressedBlobRef`

Design requirements:

- Track outside data versions used in computations.
- Permit replaying computation with historical inputs.
- Store raw inputs and intermediate transformations when configured.

## Phase 6: Audit, controls, and compliance support

### 1. Audit trail

Deliverables:

- `AuditEvent`
- `AuditActor`
- `AuditReason`
- `AuditTrailStore`
- `TamperEvidence`
- `HashChain`
- `AuditQuery`

Design requirements:

- Record what, when, who/what, and why.
- Cover money movements, manual interventions, config changes, permission changes, and decision outputs.
- Append-only default.
- Optional hash-chain tamper evidence.

### 2. Decision provenance

Deliverables:

- `DecisionRecord`
- `RuleEvaluationTrace`
- `ComplianceDecision`
- `RiskScoreRecord`
- `ManualReviewRecord`

Design requirements:

- Record not only result but how the result was reached.
- Support integration with rules engines later.

### 3. Access control and maker-checker

Deliverables:

- `Permission`
- `Role`
- `Grant`
- `AccessReview`
- `MakerCheckerRequest`
- `ApprovalPolicy`
- `BreakGlassOverride`

Design requirements:

- Least privilege by design.
- Authorization changes are audit events.
- Sensitive actions can require a second approver.
- Break-glass path must be explicit and heavily audited.

### 4. Compliance data boundaries

Deliverables:

- `PiiReference`
- `RetentionPolicyRef`
- `ErasureRequestRecord`
- `CryptoShreddingKeyRef`
- `JurisdictionContext`

Design requirements:

- Separate financial records from PII references.
- Do not hardcode universal retention rules.
- Make jurisdiction and policy explicit.
- Support legal/compliance-defined retention behavior.

## Phase 7: Domain-specific modules inside the monolith

### 1. Payments and cards

Deliverables:

- `PaymentIntent`
- `AuthorizationHold`
- `Capture`
- `Chargeback`
- `PspSettlementBatch`
- `CardDepositFlow`

Concerns covered:

- Auth vs capture.
- PSP webhooks.
- Clearing account posting.
- Chargeback reversal.
- Settlement reconciliation.

### 2. Banking and ACH-like flows

Deliverables:

- `BankTransferInstruction`
- `AchDebitFlow`
- `BalanceCheckObservation`
- `BankSettlementEvent`

Concerns covered:

- Balance checks are hints, not guarantees.
- Delayed settlement.
- Failed debit and overdraft consequences.

### 3. Crypto custody

Deliverables:

- `BlockchainNetwork`
- `TokenAsset`
- `WalletAddress`
- `OnChainTransaction`
- `ConfirmationPolicy`
- `ReorgRiskPolicy`
- `NetworkFeeEstimate`
- `CryptoWithdrawalFlow`

Concerns covered:

- Chain + contract asset identity.
- Network fees.
- Finality/confirmation.
- Reorgs.
- On-chain reconciliation.

### 4. FX and conversion

Deliverables:

- `FxQuote`
- `FxRate`
- `RateDirection`
- `RateSource`
- `ReferenceRate`
- `TransactionalConversion`
- `SpreadBooking`

Concerns covered:

- Directional rates.
- Bid/ask spread.
- Rate timestamps.
- Explicit source.
- Conversion rounding and residuals.

### 5. Trading and capital markets

Deliverables:

- `Instrument`
- `Order`
- `Execution`
- `Fill`
- `Position`
- `TickSize`
- `MarketDataAmount`
- `SettlementInstruction`

Concerns covered:

- Orders and fills.
- Tick sizes.
- Position tracking.
- Performance-sensitive numeric representations.
- Distinction between market data precision and ledger precision.

### 6. Quant and risk support

Deliverables:

- `ApproximateAmount`
- `RiskMetric`
- `ValuationResult`
- `Tolerance`
- `ApproximationPolicy`

Concerns covered:

- Explicitly mark approximate calculations.
- Allow `double`-based modeling where appropriate.
- Require conversion boundary before booking/accounting.

## Phase 8: Storage and adapters

### Storage interfaces

Deliverables:

- `LedgerStore`
- `AuditStore`
- `IdempotencyStore`
- `ReservationStore`
- `WorkflowStore`
- `OutboxStore`
- `ReconciliationStore`
- `RawInputStore`

Implementation strategy:

- Start with interfaces and in-memory implementations for tests.
- Add JDBC/PostgreSQL adapters after core semantics stabilize.
- Keep transaction boundary abstractions explicit.
- Do not pretend eventual consistency is safe for reservation or idempotency barriers.

## Phase 9: Serialization and API contracts

Deliverables:

- Jackson serializers/deserializers for money primitives.
- JSON schema generators or documented schemas.
- Strict parsers for string decimal and mantissa/scale formats.
- Round-trip tests for every representation.

Design requirements:

- Avoid accidental JSON number precision loss.
- Preserve scale where semantically relevant.
- Reject arbitrary unknown currency/asset codes at boundaries unless explicitly configured.

## Phase 10: Testing toolkit

Deliverables:

- `LedgerPropertyTests`
- `MoneyGenerators`
- `WorkflowCrashInjector`
- `RetryInjector`
- `WebhookReplayHarness`
- `ReconciliationScenarioBuilder`
- `GoldenCalculationTestSupport`
- `BackwardCompatibilityCorpus`

Testing goals:

- Generated posting sequences must balance.
- Duplicate operations must collapse into one effect.
- Workflow crash between any two steps must be resumable.
- Serialization round trips must preserve value.
- Old event payloads must remain readable.
- Reconciliation should catch missing/mismatched records.

## Example end-to-end flows to ship as samples

1. Crypto withdrawal
   - Idempotency key.
   - Reserve amount + estimated fee.
   - Compliance gate.
   - Broadcast idempotently.
   - Wait for finality.
   - Ledger posting.
   - On-chain reconciliation.

2. Card deposit
   - Create PSP payment with idempotency key.
   - Authorization hold.
   - Webhook persisted as raw hint.
   - Confirm against PSP API.
   - Book through clearing account.
   - Batch settlement reconciliation.
   - Chargeback reversal.

3. In-app FX conversion with cashback
   - Directional quote.
   - Reserve source funds.
   - Explicit conversion and rounding.
   - Book spread as revenue.
   - Fund cashback from promo expense account.
   - Notify through outbox.

4. ACH debit with unreliable balance observation
   - Treat balance check as non-authoritative.
   - Submit debit.
   - Handle settlement failure.
   - Detect overdraft.
   - Reconcile bank result.

## Documentation plan

Create documentation alongside implementation:

- `docs/principles.md`
- `docs/money-representation.md`
- `docs/ledger.md`
- `docs/idempotency.md`
- `docs/workflows.md`
- `docs/webhooks.md`
- `docs/reconciliation.md`
- `docs/compliance-boundaries.md`
- `docs/domain-tradeoffs.md`

Each doc should include:

- What problem this package solves.
- What it deliberately does not solve.
- Safe defaults.
- Dangerous tradeoffs.
- Example usage.

## Implementation order

Recommended order:

1. Money, currency, rounding, serialization.
2. Ledger, accounts, postings, corrections.
3. Audit trail and time model.
4. Idempotency and reservation.
5. Workflow engine interfaces.
6. Webhook/raw input/outbox patterns.
7. Reconciliation engine.
8. Access controls and maker-checker.
9. Domain flows: payments, crypto, FX, banking.
10. Trading/quant support.
11. JDBC/PostgreSQL adapters.
12. Full testing toolkit and examples.

## Major risks

1. **Over-abstraction**
   - Fintech domains differ too much for one perfect model. Keep domain choices explicit.

2. **False sense of compliance**
   - Library must provide tools, not legal rules.

3. **Unsafe defaults**
   - Avoid default rounding, default retention, default trust in webhooks, or default idempotency windows.

4. **Performance vs correctness confusion**
   - Clearly separate accounting-grade types from market-data/quant types.

5. **Storage semantics mismatch**
   - Some operations require strong consistency. Adapter docs must state required isolation/locking semantics.

6. **Monolith sprawl**
   - Keep internal package boundaries strict even though artifact is monolithic.

## First milestone definition

Milestone 1 should produce a small but coherent core:

- Money and asset primitives.
- Explicit rounding policies.
- JSON-safe serialization.
- Basic double-entry ledger.
- Immutable postings.
- Reversal/correction support.
- In-memory stores.
- Property tests proving balanced postings and safe arithmetic.
- Documentation for representation tradeoffs.

This milestone gives the rest of the library a safe foundation.
