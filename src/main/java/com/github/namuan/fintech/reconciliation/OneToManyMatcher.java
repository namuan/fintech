package com.github.namuan.fintech.reconciliation;
import java.math.BigDecimal; import java.util.List;
public final class OneToManyMatcher { public boolean matches(ReconciliationRecord one, List<ReconciliationRecord> many){ if(many.isEmpty()) return false; if(many.stream().anyMatch(r -> !r.amount().asset().equals(one.amount().asset()))) return false; BigDecimal sum = many.stream().map(r -> r.amount().decimalValue()).reduce(BigDecimal.ZERO, BigDecimal::add); return sum.compareTo(one.amount().decimalValue()) == 0; } }
