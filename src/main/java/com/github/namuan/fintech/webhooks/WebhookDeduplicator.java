package com.github.namuan.fintech.webhooks;
import java.util.Set; import java.util.concurrent.ConcurrentHashMap;
public final class WebhookDeduplicator { private final Set<String> seen = ConcurrentHashMap.newKeySet(); public boolean firstDelivery(WebhookEnvelope e){ return seen.add(e.id()); } }
