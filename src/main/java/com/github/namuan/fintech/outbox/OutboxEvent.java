package com.github.namuan.fintech.outbox;
import java.time.Instant; import java.util.Map;
public record OutboxEvent(PublishedEventId id, String topic, String payload, Instant createdAt, Map<String,String> headers) { public OutboxEvent { headers = headers == null ? Map.of() : Map.copyOf(headers); } }
