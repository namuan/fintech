package com.github.namuan.fintech.reconciliation;
import java.time.Duration;
public record HeuristicMatcher(Duration timeTolerance) implements MatchRule { public boolean matches(ReconciliationRecord l, ReconciliationRecord r){ return l.amount().asset().equals(r.amount().asset()) && l.amount().decimalValue().compareTo(r.amount().decimalValue()) == 0 && Math.abs(Duration.between(l.effectiveAt(), r.effectiveAt()).toMillis()) <= timeTolerance.toMillis(); } }
