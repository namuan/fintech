package com.github.namuan.fintech.outbox;
import java.util.*; import java.util.concurrent.ConcurrentHashMap;
public final class InMemoryOutboxStore implements OutboxStore { private final Map<PublishedEventId,OutboxEvent> pending = new ConcurrentHashMap<>(); public void append(OutboxEvent e){pending.put(e.id(),e);} public List<OutboxEvent> pending(){return List.copyOf(pending.values());} public void markPublished(PublishedEventId id){pending.remove(id);} }
