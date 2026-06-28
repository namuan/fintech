package com.github.namuan.fintech.audit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
public final class InMemoryAuditTrailStore implements AuditTrailStore {
    private final CopyOnWriteArrayList<AuditEvent> events = new CopyOnWriteArrayList<>();
    public void append(AuditEvent event) { events.add(event); }
    public List<AuditEvent> all() { return List.copyOf(events); }
}
