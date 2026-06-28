package com.github.namuan.fintech.reconciliation;
@FunctionalInterface public interface MatchRule { boolean matches(ReconciliationRecord left, ReconciliationRecord right); static MatchRule sameIdAndAmount(){ return (l,r) -> l.id().equals(r.id()) && l.amount().decimalValue().compareTo(r.amount().decimalValue()) == 0 && l.amount().asset().equals(r.amount().asset()); } }
