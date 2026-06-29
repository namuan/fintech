package com.github.namuan.fintech.storage.jdbc;

import com.github.namuan.fintech.webhooks.RawWebhookStore;
import com.github.namuan.fintech.webhooks.WebhookEnvelope;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcRawWebhookStore implements RawWebhookStore {
    private final DataSource dataSource;
    public JdbcRawWebhookStore(DataSource dataSource) { this.dataSource = dataSource; }
    @Override public void append(WebhookEnvelope envelope) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("insert into fintech_raw_webhooks(id,provider_id,received_at,headers_json,raw_body) values(?,?,?,?,?) on conflict do nothing")) {
            statement.setString(1, envelope.id()); statement.setString(2, envelope.providerId()); JdbcSupport.setInstant(statement, 3, envelope.receivedAt()); statement.setString(4, JdbcSupport.toJson(envelope.headers())); statement.setBytes(5, envelope.rawBody()); statement.executeUpdate();
        } catch (SQLException e) { throw new IllegalArgumentException("Could not append raw webhook", e); }
    }
    @Override public Optional<WebhookEnvelope> find(String id) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("select * from fintech_raw_webhooks where id=?")) { statement.setString(1, id); try (var rs = statement.executeQuery()) { return rs.next() ? Optional.of(envelope(rs)) : Optional.empty(); } }
        catch (SQLException e) { throw new IllegalArgumentException("Could not load raw webhook", e); }
    }
    @Override public List<WebhookEnvelope> all() {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("select * from fintech_raw_webhooks order by received_at,id"); var rs = statement.executeQuery()) { List<WebhookEnvelope> values = new ArrayList<>(); while (rs.next()) values.add(envelope(rs)); return values; }
        catch (SQLException e) { throw new IllegalArgumentException("Could not load raw webhooks", e); }
    }
    private static WebhookEnvelope envelope(java.sql.ResultSet rs) throws SQLException { return new WebhookEnvelope(rs.getString("id"), rs.getString("provider_id"), JdbcSupport.instant(rs, "received_at"), JdbcSupport.stringMap(rs.getString("headers_json")), rs.getBytes("raw_body")); }
}
