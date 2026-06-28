package com.github.namuan.fintech.outbox;
public final class OutboxRelay { private final OutboxStore store; private final OutboxPublisher publisher; public OutboxRelay(OutboxStore store, OutboxPublisher publisher){this.store=store;this.publisher=publisher;} public void drain() throws Exception { for (var e: store.pending()){ publisher.publish(e); store.markPublished(e.id()); } } }
