package com.github.namuan.fintech.controls;
import java.time.Instant;
public record BreakGlassOverride(String id, String actorId, String action, String justification, Instant occurredAt) {}
