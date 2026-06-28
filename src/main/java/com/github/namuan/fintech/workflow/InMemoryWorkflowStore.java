package com.github.namuan.fintech.workflow;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
public final class InMemoryWorkflowStore implements WorkflowStore {
    private final ConcurrentHashMap<String, WorkflowState> states = new ConcurrentHashMap<>();
    public void save(WorkflowState state) { states.put(state.workflowId(), state); }
    public Optional<WorkflowState> load(String workflowId) { return Optional.ofNullable(states.get(workflowId)); }
}
