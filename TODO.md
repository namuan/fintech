# Implementation TODO

## In progress

- None.

## Pending

- [ ] Add JDBC/PostgreSQL adapters after storage semantics are finalized.

## Completed

- [x] Created implementation todo list.
- [x] Set up Maven Java 21 project structure.
- [x] Implement core money, currency, asset, amount, rounding, and serialization primitives.
- [x] Implement ledger, journal entries, postings, trial balance, corrections, reversals, and in-memory stores.
- [x] Implement audit and financial time primitives.
- [x] Implement idempotency primitives and in-memory idempotency barrier.
- [x] Implement funds reservation primitives and in-memory reservation service.
- [x] Implement workflow interfaces and simple durable in-memory driver/store.
- [x] Add tests for money arithmetic, JSON-safe serialization, ledger balancing, corrections, idempotency, reservations, and workflow resume behavior.
- [x] Run Maven verification and fix issues.
- [x] Implement FX, integrations, webhooks, outbox, reconciliation, and lineage packages.
- [x] Implement controls, compliance, payments, banking, crypto, trading, and quant support.
- [x] Add tests for remaining packages.
- [x] Run Maven verification and fix issues.
- [x] Add documentation pages (principles, money representation, ledger, idempotency, workflows, webhooks, reconciliation, compliance boundaries, domain tradeoffs).
- [x] Add README.md with package map, quick start, and license link.
- [x] Add docs/index.md with reading order and topic index.
- [x] Add deeper property-based tests for money, ledger, idempotency, workflow resume, serialization, and reconciliation invariants.
