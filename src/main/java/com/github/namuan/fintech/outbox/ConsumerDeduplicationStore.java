package com.github.namuan.fintech.outbox;
public interface ConsumerDeduplicationStore { boolean markIfFirst(PublishedEventId id); }
