package com.github.namuan.fintech.outbox;
import java.util.UUID;
public record PublishedEventId(String value) { public static PublishedEventId random(){ return new PublishedEventId(UUID.randomUUID().toString()); } }
