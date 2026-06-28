package com.github.namuan.fintech.reconciliation;
import com.github.namuan.fintech.money.Money; import java.time.Instant; import java.util.Map;
public record ReconciliationRecord(String id, String source, Money amount, Instant effectiveAt, Map<String,String> attributes) { public ReconciliationRecord { attributes = attributes == null ? Map.of() : Map.copyOf(attributes); } }
