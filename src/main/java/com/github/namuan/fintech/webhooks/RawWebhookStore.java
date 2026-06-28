package com.github.namuan.fintech.webhooks;
import java.util.List; import java.util.Optional;
public interface RawWebhookStore { void append(WebhookEnvelope envelope); Optional<WebhookEnvelope> find(String id); List<WebhookEnvelope> all(); }
