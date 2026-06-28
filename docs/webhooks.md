# Webhooks

## What problem this package solves

Webhooks are the most common way to receive signals from external systems — payment providers, banks, custodians, KYC vendors. But webhooks arrive out of order, carry stale data, get duplicated, and sometimes never arrive at all. Processing them as if they were trustworthy facts leads to silent data corruption.

The `webhooks` package enforces a defensive pipeline: verify the caller, persist the raw payload, acknowledge fast, and process asynchronously. It treats every webhook as a **hint** that something happened, not a trustworthy account of what happened.

## Package layout

```
com.github.namuan.fintech.webhooks
  WebhookEnvelope          – raw bytes + headers + provider id + received timestamp
  RawWebhookStore          – append / find interface for durable storage of raw payloads
  InMemoryRawWebhookStore  – thread‑safe ConcurrentHashMap implementation
  WebhookSignatureVerifier – @FunctionalInterface: verify the caller
  WebhookDeduplicator      – tracks already‑seen webhook ids
  WebhookProcessor         – @FunctionalInterface: the async business‑logic callback
  WebhookIngestionService  – verifies signature, stores raw envelope, returns 2xx
  WebhookAsHintPolicy      – REQUIRE_AUTHORITATIVE_API_CONFIRMATION or TRUST_AFTER_SIGNATURE_FOR_LOW_RISK
```

## What it deliberately does not solve

- The library does **not** implement HMAC‑SHA256 or asymmetric signature verification. `WebhookSignatureVerifier` is a functional interface. Callers inject their own verification logic. A `trustForTests()` factory is provided for test environments.
- It does **not** provide an HTTP endpoint or servlet. The `WebhookIngestionService.ingest` method is a plain Java call. Wrap it in a Spring controller, JAX‑RS resource, or raw `HttpHandler`.
- It does **not** decide which events to trust based on payload content. `WebhookAsHintPolicy` signals intent, but the actual "query the authoritative API" step lives in the caller’s `WebhookProcessor`.

## Safe defaults

- The **raw payload is persisted first**, before any business logic runs. If processing crashes, the payload is still there for replay.
- `WebhookDeduplicator` prevents double‑processing of the same event id.
- `WebhookSignatureVerifier` must pass before `WebhookIngestionService` stores the envelope. No unverified payload enters the system.
- The recommended `WebhookAsHintPolicy` is `REQUIRE_AUTHORITATIVE_API_CONFIRMATION` — query the provider’s API for the actual state rather than trusting the webhook body.

## Dangerous tradeoffs

| Tradeoff | Risk | Mitigation |
|---|---|---|
| Trusting webhook content at face value | The provider’s webhook backend may carry stale or transformed data | Always reconcile against the authoritative API |
| Processing webhooks synchronously in the HTTP handler | Slow processing causes the provider to time out and retry, multiplying load | Persist raw payload, return 2xx, process asynchronously |
| Assuming webhooks are always delivered | A dropped webhook means a missed fact — e.g. a "captured" event that never arrives | Run a periodic reconciliation job that queries the provider’s API directly |
| Assuming ordering | A "refunded" webhook arriving before the "captured" webhook corrupts state | Design your processor to handle out‑of‑order events; use the API as the authoritative timeline |
| Verifying signature on re‑serialised payload | Re‑serialising JSON changes whitespace and breaks the HMAC | Verify over the raw bytes you received, not a parsed‑and‑re‑serialised version |

## Example usage

```java
// 1. Ingest the webhook (in your HTTP handler)
RawWebhookStore store = new InMemoryRawWebhookStore();
WebhookSignatureVerifier verifier = (envelope) -> {
    byte[] secret = loadSharedSecret(envelope.providerId());
    return HmacUtils.verify(envelope.rawBody(), envelope.headers().get("X-Signature"), secret);
};
WebhookIngestionService ingestion = new WebhookIngestionService(store, verifier);

WebhookEnvelope envelope = new WebhookEnvelope(
    "evt-123", "stripe", Instant.now(),
    Map.of("X-Signature", request.getHeader("X-Signature")),
    request.getBody().getBytes()
);
ingestion.ingest(envelope); // throws if signature invalid

// 2. Process asynchronously (in a worker thread)
WebhookDeduplicator dedup = new WebhookDeduplicator();
WebhookProcessor processor = (env) -> {
    if (!dedup.firstDelivery(env)) return;
    // Query Stripe API for authoritative state
    PaymentState state = stripeClient.getPayment(extractPaymentId(env));
    // Update internal ledger based on confirmed state
    ledger.updatePayment(state);
};

for (var stored : store.all()) {
    processor.process(stored); // safe to rerun — deduplicator and idempotent ledger
}
```

## Read next

- [Reconciliation](reconciliation.md) — the safety net that catches the missing webhook
- [Idempotency](idempotency.md) — making webhook processing safe to rerun
- [Principles](principles.md) — the "no trust" principle and why webhooks are treated as hints
