package com.github.namuan.fintech.workflow;

import java.util.Map;

public final class WorkflowDriver {
    private final WorkflowStore store;
    public WorkflowDriver(WorkflowStore store) { this.store = store; }
    public WorkflowState run(String workflowId, MoneyWorkflow workflow) throws Exception {
        WorkflowState state = store.load(workflowId).orElse(new WorkflowState(workflowId, "START", false, Map.of()));
        for (NamedWorkflowStep namedStep : workflow.steps()) {
            if (state.completed()) return state;
            state = new WorkflowState(workflowId, namedStep.name(), false, state.data());
            store.save(state);
            state = namedStep.step().run(state);
            store.save(state);
        }
        WorkflowState completed = new WorkflowState(workflowId, "DONE", true, state.data());
        store.save(completed);
        return completed;
    }
}
