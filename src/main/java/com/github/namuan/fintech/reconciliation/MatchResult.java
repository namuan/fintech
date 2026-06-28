package com.github.namuan.fintech.reconciliation;
import java.util.List;
public record MatchResult(List<ReconciliationRecord> left, List<ReconciliationRecord> right, boolean matched, String reason) {}
