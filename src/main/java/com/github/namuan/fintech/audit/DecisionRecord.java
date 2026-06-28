package com.github.namuan.fintech.audit;
import java.time.Instant; import java.util.Map;
public record DecisionRecord(String id, String decisionType, String result, Instant decidedAt, Map<String,String> inputs) { public DecisionRecord { inputs = inputs == null ? Map.of() : Map.copyOf(inputs); } }
