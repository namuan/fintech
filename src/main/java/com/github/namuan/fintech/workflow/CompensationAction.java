package com.github.namuan.fintech.workflow;
@FunctionalInterface public interface CompensationAction { void compensate(WorkflowState failedState) throws Exception; }
