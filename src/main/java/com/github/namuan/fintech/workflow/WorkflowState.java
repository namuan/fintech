package com.github.namuan.fintech.workflow;
import java.util.Map;
public record WorkflowState(String workflowId, String stepName, boolean completed, Map<String, String> data) {
    public WorkflowState { data = data == null ? Map.of() : Map.copyOf(data); }
}
