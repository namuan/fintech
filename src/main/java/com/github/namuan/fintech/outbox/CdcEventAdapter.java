package com.github.namuan.fintech.outbox;
@FunctionalInterface public interface CdcEventAdapter<T> { OutboxEvent toPublicEvent(T change); }
