package com.github.namuan.fintech.workflow;
import java.util.Optional;
public interface WorkflowStore { void save(WorkflowState state); Optional<WorkflowState> load(String workflowId); }
