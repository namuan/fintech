package com.github.namuan.fintech.webhooks;
import java.time.Instant; import java.util.Map;
public record WebhookEnvelope(String id, String providerId, Instant receivedAt, Map<String,String> headers, byte[] rawBody) { public WebhookEnvelope { headers = headers == null ? Map.of() : Map.copyOf(headers); rawBody = rawBody == null ? new byte[0] : rawBody.clone(); } @Override public byte[] rawBody(){return rawBody.clone();}}
