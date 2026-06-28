package com.github.namuan.fintech.outbox;
@FunctionalInterface public interface OutboxPublisher { void publish(OutboxEvent event) throws Exception; }
