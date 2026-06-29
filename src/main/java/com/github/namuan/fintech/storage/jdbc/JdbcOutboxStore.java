package com.github.namuan.fintech.storage.jdbc;

import com.github.namuan.fintech.outbox.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class JdbcOutboxStore implements OutboxStore {
    private final DataSource dataSource;
    public JdbcOutboxStore(DataSource dataSource) { this.dataSource = dataSource; }
    @Override public void append(OutboxEvent event) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("insert into fintech_outbox_events(id,topic,payload,created_at,headers_json) values(?,?,?,?,?::jsonb) on conflict do nothing")) {
            statement.setString(1, event.id().value()); statement.setString(2, event.topic()); statement.setString(3, event.payload()); JdbcSupport.setInstant(statement, 4, event.createdAt()); statement.setString(5, JdbcSupport.toJson(event.headers()));
            if (statement.executeUpdate() == 0) throw new IllegalArgumentException("Outbox event already exists: " + event.id().value());
        } catch (SQLException e) { throw new IllegalArgumentException("Could not append outbox event", e); }
    }
    @Override public List<OutboxEvent> pending() {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("select * from fintech_outbox_events where published_at is null order by created_at,id"); var rs = statement.executeQuery()) { List<OutboxEvent> events = new ArrayList<>(); while (rs.next()) events.add(event(rs)); return events; }
        catch (SQLException e) { throw new IllegalArgumentException("Could not load pending outbox events", e); }
    }
    @Override public void markPublished(PublishedEventId id) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("update fintech_outbox_events set published_at=now() where id=?")) { statement.setString(1, id.value()); statement.executeUpdate(); }
        catch (SQLException e) { throw new IllegalArgumentException("Could not mark outbox event published", e); }
    }
    private static OutboxEvent event(java.sql.ResultSet rs) throws SQLException { return new OutboxEvent(new PublishedEventId(rs.getString("id")), rs.getString("topic"), rs.getString("payload"), JdbcSupport.instant(rs, "created_at"), JdbcSupport.stringMap(rs.getString("headers_json"))); }
}
