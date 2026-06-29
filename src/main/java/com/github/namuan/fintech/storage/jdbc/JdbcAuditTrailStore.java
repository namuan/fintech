package com.github.namuan.fintech.storage.jdbc;

import com.github.namuan.fintech.audit.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcAuditTrailStore implements AuditTrailStore {
    private final DataSource dataSource;
    public JdbcAuditTrailStore(DataSource dataSource) { this.dataSource = dataSource; }
    @Override public void append(AuditEvent event) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("insert into fintech_audit_events(id,occurred_at,actor_type,actor_id,action,reason_code,reason_description,data_json) values(?,?,?,?,?,?,?,?::jsonb)")) {
            statement.setString(1, event.id()); JdbcSupport.setInstant(statement, 2, event.occurredAt()); statement.setString(3, event.actor().type()); statement.setString(4, event.actor().id()); statement.setString(5, event.action()); statement.setString(6, event.reason().code()); statement.setString(7, event.reason().description()); statement.setString(8, JdbcSupport.toJson(event.data())); statement.executeUpdate();
        } catch (SQLException e) { throw new IllegalArgumentException("Could not append audit event", e); }
    }
    @Override public List<AuditEvent> all() {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("select * from fintech_audit_events order by occurred_at,id"); var rs = statement.executeQuery()) { List<AuditEvent> events = new ArrayList<>(); while (rs.next()) events.add(new AuditEvent(rs.getString("id"), JdbcSupport.instant(rs, "occurred_at"), new AuditActor(rs.getString("actor_type"), rs.getString("actor_id")), rs.getString("action"), new AuditReason(rs.getString("reason_code"), rs.getString("reason_description")), JdbcSupport.stringMap(rs.getString("data_json")))); return events; }
        catch (SQLException e) { throw new IllegalArgumentException("Could not load audit events", e); }
    }
}
