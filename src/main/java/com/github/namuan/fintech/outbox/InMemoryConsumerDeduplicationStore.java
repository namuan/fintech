package com.github.namuan.fintech.outbox;
import java.util.Set; import java.util.concurrent.ConcurrentHashMap;
public final class InMemoryConsumerDeduplicationStore implements ConsumerDeduplicationStore { private final Set<PublishedEventId> seen = ConcurrentHashMap.newKeySet(); public boolean markIfFirst(PublishedEventId id){ return seen.add(id); } }
