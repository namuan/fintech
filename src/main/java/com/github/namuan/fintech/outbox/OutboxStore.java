package com.github.namuan.fintech.outbox;
import java.util.List;
public interface OutboxStore { void append(OutboxEvent event); List<OutboxEvent> pending(); void markPublished(PublishedEventId id); }
