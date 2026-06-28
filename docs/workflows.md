# Workflows

## What problem this package solves

A money operation is rarely a single write. A withdrawal might reserve funds, run compliance checks, broadcast to a blockchain, wait for confirmations, and then post to the ledger — five steps that can crash between any two. A flow that assumes it runs to completion in one go will leave money stranded, double‑sent, or silently lost.

The `workflow` package models money flows as an explicit state machine with durable persistence. Every step commits its progress before starting the next. An independent `WorkflowDriver` can pick up a half‑finished flow where it left off.

## Package layout

```
com.github.namuan.fintech.workflow
  WorkflowState        – (workflowId, stepName, completed, data) — persisted before and after each step
  WorkflowStep         – @FunctionalInterface: run the step, return the next state
  NamedWorkflowStep    – (name, WorkflowStep) — gives each step a stable identifier
  MoneyWorkflow        – (name, List<NamedWorkflowStep>) — the ordered definition
  WorkflowStore        – load / save interface
  InMemoryWorkflowStore – thread‑safe ConcurrentHashMap implementation
  WorkflowDriver       – runs a workflow from start to finish, resuming at the last persisted step
  RetryPolicy          – maxAttempts for transient‑error retries
  CompensationAction   – @FunctionalInterface: undo a previous step’s external effect
  Saga                 – a MoneyWorkflow paired with compensation actions for each step
```

## What it deliberately does not solve

- The `WorkflowDriver` is **single‑threaded and in‑process**. It does not schedule retries across restarts, scale across nodes, or handle backpressure. For production, swap in a durable‑execution engine adapter (Temporal, Camunda, AWS Step Functions).
- Steps are **not automatically idempotent**. The driver persists state before and after each step, but the step implementation itself must be safe to rerun (see Idempotency). For example, a "broadcast transaction" step must re‑check the chain before sending.
- The `Saga` type provides the compensation hook but the driver does not automatically invoke compensations on failure. Callers wire the error‑handling logic themselves.
- There is **no built‑in timeout or expiry** for stalled workflows. An external scheduler or cron job should poll the `WorkflowStore` for incomplete flows.

## Safe defaults

- State is persisted **before** a step runs and **after** it completes. If the process dies mid‑step, the `stepName` in the persisted state tells the resumer exactly where to pick up.
- `WorkflowState.data` is an immutable `Map<String, String>` — small, serialisable, and safe for consistent hashing.
- The `InMemoryWorkflowStore` keeps all states in a `ConcurrentHashMap`, making it suitable for tests and local development.

## Dangerous tradeoffs

| Tradeoff | Risk | Mitigation |
|---|---|---|
| Relying on in‑memory state in production | A process restart loses all in‑flight workflows | Use a durable store (JDBC, Redis, or a workflow engine adapter) |
| Forgetting to make a step idempotent | On resume, the step re‑executes and double‑broadcasts | Wrap every step with an `IdempotencyBarrier` or check external state before acting |
| Not wiring compensations for external calls | A cancelled flow leaves a PSP charge that was never refunded | Model the flow as a `Saga` and invoke compensations in the error handler |
| Running the driver in a request thread | A long‑running flow blocks the HTTP response | Run the driver asynchronously; return a "processing" status to the caller |

## Example usage

```java
InMemoryWorkflowStore store = new InMemoryWorkflowStore();
WorkflowDriver driver = new WorkflowDriver(store);

MoneyWorkflow cryptoWithdrawal = new MoneyWorkflow("crypto-withdrawal", List.of(
    new NamedWorkflowStep("reserve", state -> {
        // Reserve 0.5 ETH + estimated fee against available balance
        log.info("Reserving funds for {}", state.workflowId());
        return new WorkflowState(state.workflowId(), "reserve", false,
            Map.of("reservationId", "res-xyz"));
    }),
    new NamedWorkflowStep("compliance", state -> {
        // Run AML/sanctions checks — may take hours or days
        String reservationId = state.data().get("reservationId");
        boolean passed = complianceService.screen(reservationId);
        return new WorkflowState(state.workflowId(), "compliance", false,
            Map.of("reservationId", reservationId, "compliance", String.valueOf(passed)));
    }),
    new NamedWorkflowStep("broadcast", state -> {
        // Broadcast on‑chain — must be idempotent
        String txHash = chainService.broadcast(reservationId);
        return new WorkflowState(state.workflowId(), "broadcast", false,
            Map.of("txHash", txHash));
    }),
    new NamedWorkflowStep("settle", state -> {
        // Wait for finality, post to ledger, release remaining reserve
        ledgerService.settle(state.data().get("txHash"));
        return new WorkflowState(state.workflowId(), "settle", false, Map.of());
    })
));

WorkflowState done = driver.run("wf-withdraw-42", cryptoWithdrawal);
assert done.completed(); // true
```

## Read next

- [Idempotency](idempotency.md) — why every workflow step must be safe to rerun
- [Ledger](ledger.md) — where the final posting happens
- [Reconciliation](reconciliation.md) — the safety net that verifies the outcome
