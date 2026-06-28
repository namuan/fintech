package com.github.namuan.fintech.quant;
import java.time.Instant; import java.util.List;
public record ValuationResult(String id, ApproximateAmount value, List<RiskMetric> metrics, Instant valuedAt) { public ValuationResult { metrics = List.copyOf(metrics); } }
