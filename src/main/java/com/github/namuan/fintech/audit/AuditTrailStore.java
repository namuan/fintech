package com.github.namuan.fintech.audit;
import java.util.List;
public interface AuditTrailStore { void append(AuditEvent event); List<AuditEvent> all(); }
