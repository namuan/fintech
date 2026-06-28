# Compliance boundaries

## What problem this package solves

Financial systems must comply with data‑protection laws (GDPR, CCPA), anti‑money‑laundering regulations, and audit requirements — often with conflicting demands. GDPR says "delete personal data on request"; accounting law says "keep financial records for 5–10 years." The `compliance` package provides primitives for separating PII from immutable financial data, modelling retention policies, recording erasure requests, and making jurisdiction explicit — without hardcoding any jurisdiction’s rules.

## Package layout

```
com.github.namuan.fintech.compliance
  PiiReference          – (subjectId, piiStoreRef) — an opaque pointer to personal data
  RetentionPolicyRef    – (policyId, description) — identifies the retention rule, not its duration
  ErasureRequestRecord  – (id, subject, requestedAt, legalBasis) — a recorded right‑to‑erasure request
  CryptoShreddingKeyRef – (subjectId, keyId, destroyed) — erasure by key deletion
  JurisdictionContext   – (countryCode, regionCode, retentionPolicy) — makes regulatory scope explicit

com.github.namuan.fintech.controls
  Permission            – a named capability
  Role                  – a named collection of permissions
  Grant                 – (actorId, role, grantedBy, grantedAt) — an auditable permission assignment
  AccessReview          – (id, reviewedAt, reviewerId, grants) — periodic recertification record
  ApprovalPolicy        – (requiredApprovers, requesterMayApprove) — maker‑checker configuration
  MakerCheckerRequest   – a sensitive action that needs approval before it takes effect
  BreakGlassOverride    – (id, actorId, action, justification, occurredAt) — heavily‑audited emergency path

com.github.namuan.fintech.audit (decision provenance extensions)
  DecisionRecord        – (id, decisionType, result, decidedAt, inputs) — what was decided and why
  RuleEvaluationTrace   – (decisionId, firedRules) — which rules fired to reach the decision
  ComplianceDecision    – bundles a DecisionRecord with its RuleEvaluationTrace
  RiskScoreRecord       – (subjectId, score, modelVersion) — a scored risk assessment
  ManualReviewRecord    – (id, reviewerId, outcome, reviewedAt) — a human decision
```

## What it deliberately does not solve

- The library does **not** encode retention durations, AML thresholds, or sanction lists. These are jurisdiction‑specific and must be configured by the adopter’s legal and compliance teams.
- It does **not** implement cryptographic erasure. `CryptoShreddingKeyRef` marks a key as destroyed; the actual key deletion and encryption are the caller’s responsibility.
- It does **not** enforce access control at runtime. The controls package provides **data structures** for permissions, roles, grants, and approvals. Integrating them with an authentication/authorisation framework (Spring Security, OPA, etc.) is the caller’s job.
- It does **not** connect to a rules engine (Drools, DMN, Decisions4s). `RuleEvaluationTrace` records which rules fired; populating it from an actual engine is the caller’s responsibility.

## Safe defaults

- `PiiReference` is an opaque pointer. The immutable financial ledger references `subjectId`, not names, addresses, or documents.
- `ErasureRequestRecord` is itself an audit record. Simply deleting a row is not enough; the fact that an erasure request was received and acted upon must be traceable.
- `ApprovalPolicy.requesterMayApprove` defaults to `false` — the person who requests a sensitive action cannot also approve it.
- `BreakGlassOverride` requires a `justification` string. Emergency access must be explained and audited.
- `JurisdictionContext` makes the legal scope explicit for every record so that retention and erasure logic can be applied correctly.

## Dangerous tradeoffs

| Tradeoff | Risk | Mitigation |
|---|---|---|
| Hardcoding retention rules in the library | A rule valid in the EU is illegal in Singapore | Model retention as a `RetentionPolicyRef`; let the adopter configure actual durations |
| Embedding PII directly in the immutable ledger | An erasure request forces rewriting financial history | Reference subjects by opaque id; store PII in a separate mutable store |
| Skipping access reviews | Stale permissions accumulate; ex‑employees retain access | Schedule periodic `AccessReview` runs; revoke grants that are no longer needed |
| Not providing a break‑glass path | Operators route around rigid controls in an emergency | Model `BreakGlassOverride` explicitly; audit it heavily; never leave a backdoor |
| Assuming all jurisdictions allow crypto‑shredding | Some regulators require the ability to reconstruct data even after "erasure" | Consult your legal team; do not rely on crypto‑shredding alone unless permitted |

## Example usage

```java
// Separate PII from financial data
PiiReference userPii = new PiiReference("user-42", "pii-store:users/42");

// The ledger references the opaque id, not the PII
Account userAccount = new Account(AccountId.random(), "User-42 Balance", AccountType.LIABILITY, usd);
// userPii.subjectId() == "user-42" — the link is indirect

// Record an erasure request
ErasureRequestRecord erasure = new ErasureRequestRecord(
    "era-001", userPii, Instant.now(), "GDPR Art.17"
);
auditStore.append(AuditEvent.now(
    new AuditActor("system", "gdpr-handler"),
    "erasure-requested",
    new AuditReason("GDPR-17", "Right to erasure"),
    Map.of("subjectId", userPii.subjectId())
));

// Maker‑checker for a large withdrawal
MakerCheckerRequest withdrawal = new MakerCheckerRequest(
    "wdr-99", "alice", "withdraw-50000-USD", Instant.now()
);
ApprovalPolicy policy = new ApprovalPolicy(1, false); // 1 approver, requester cannot approve
withdrawal.approve("bob", policy);
assert withdrawal.approved(policy); // true
```

## Read next

- [Principles](principles.md) — the "no trust" principle applied to internal actors
- [Ledger](ledger.md) — why the immutable ledger references opaque ids
- [Domain tradeoffs](domain-tradeoffs.md) — how compliance posture differs across domains
