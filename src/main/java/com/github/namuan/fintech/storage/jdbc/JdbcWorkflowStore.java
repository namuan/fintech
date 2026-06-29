package com.github.namuan.fintech.storage.jdbc;

import com.github.namuan.fintech.workflow.WorkflowState;
import com.github.namuan.fintech.workflow.WorkflowStore;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcWorkflowStore implements WorkflowStore {
    private final DataSource dataSource;
    public JdbcWorkflowStore(DataSource dataSource) { this.dataSource = dataSource; }
    @Override public void save(WorkflowState state) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("insert into fintech_workflow_states(workflow_id,step_name,completed,data_json,updated_at) values(?,?,?,?,now()) on conflict(workflow_id) do update set step_name=excluded.step_name, completed=excluded.completed, data_json=excluded.data_json, updated_at=now()")) {
            statement.setString(1, state.workflowId()); statement.setString(2, state.stepName()); statement.setBoolean(3, state.completed()); statement.setString(4, JdbcSupport.toJson(state.data())); statement.executeUpdate();
        } catch (SQLException e) { throw new IllegalArgumentException("Could not save workflow state", e); }
    }
    @Override public Optional<WorkflowState> load(String workflowId) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("select * from fintech_workflow_states where workflow_id=?")) {
            statement.setString(1, workflowId);
            try (var rs = statement.executeQuery()) { return rs.next() ? Optional.of(new WorkflowState(rs.getString("workflow_id"), rs.getString("step_name"), rs.getBoolean("completed"), JdbcSupport.stringMap(rs.getString("data_json")))) : Optional.empty(); }
        } catch (SQLException e) { throw new IllegalArgumentException("Could not load workflow state", e); }
    }
}
