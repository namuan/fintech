package com.github.namuan.fintech.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(String id, Instant occurredAt, AuditActor actor, String action, AuditReason reason, Map<String, String> data) {
    public AuditEvent { data = data == null ? Map.of() : Map.copyOf(data); }
    public static AuditEvent now(AuditActor actor, String action, AuditReason reason, Map<String, String> data) {
        return new AuditEvent(UUID.randomUUID().toString(), Instant.now(), actor, action, reason, data);
    }
}
