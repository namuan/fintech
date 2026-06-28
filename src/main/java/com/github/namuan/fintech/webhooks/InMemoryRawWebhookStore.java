package com.github.namuan.fintech.webhooks;
import java.util.*; import java.util.concurrent.ConcurrentHashMap;
public final class InMemoryRawWebhookStore implements RawWebhookStore { private final Map<String,WebhookEnvelope> values = new ConcurrentHashMap<>(); public void append(WebhookEnvelope e){ values.putIfAbsent(e.id(), e);} public Optional<WebhookEnvelope> find(String id){return Optional.ofNullable(values.get(id));} public List<WebhookEnvelope> all(){return List.copyOf(values.values());}}
