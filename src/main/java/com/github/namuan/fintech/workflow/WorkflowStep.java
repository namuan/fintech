package com.github.namuan.fintech.workflow;
@FunctionalInterface public interface WorkflowStep { WorkflowState run(WorkflowState current) throws Exception; }
