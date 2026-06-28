package com.github.namuan.fintech.audit;
import java.util.List;
public record RuleEvaluationTrace(String decisionId, List<String> firedRules) { public RuleEvaluationTrace { firedRules = List.copyOf(firedRules); } }
