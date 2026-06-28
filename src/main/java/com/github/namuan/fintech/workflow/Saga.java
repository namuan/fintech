package com.github.namuan.fintech.workflow;
import java.util.List;
public record Saga(MoneyWorkflow workflow, List<CompensationAction> compensations) {}
