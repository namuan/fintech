package com.github.namuan.fintech.workflow;
import java.util.List;
public record MoneyWorkflow(String name, List<NamedWorkflowStep> steps) { public MoneyWorkflow { steps = List.copyOf(steps); } }
