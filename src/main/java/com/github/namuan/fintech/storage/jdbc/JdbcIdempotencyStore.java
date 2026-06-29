package com.github.namuan.fintech.storage.jdbc;

import com.github.namuan.fintech.idempotency.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;

public final class JdbcIdempotencyStore implements IdempotencyStore {
    private final DataSource dataSource;
    public JdbcIdempotencyStore(DataSource dataSource) { this.dataSource = dataSource; }

    @Override public Optional<IdempotencyRecord> find(IdempotencyScope scope, IdempotencyKey key) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("select payload_hash,result_text from fintech_idempotency_records where domain=? and operation=? and actor=? and key_value=?")) {
            bindKey(statement, scope, key);
            try (var rs = statement.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new IdempotencyRecord(scope, key, rs.getString("payload_hash"), rs.getString("result_text")));
            }
        } catch (SQLException e) { throw new IllegalArgumentException("Could not load idempotency record", e); }
    }

    @Override public IdempotencyRecord putIfAbsent(IdempotencyRecord record) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("insert into fintech_idempotency_records(domain,operation,actor,key_value,payload_hash,result_text) values(?,?,?,?,?,?) on conflict do nothing")) {
            bindKey(statement, record.scope(), record.key());
            statement.setString(5, record.payloadHash());
            statement.setString(6, record.result() == null ? null : String.valueOf(record.result()));
            if (statement.executeUpdate() == 1) return null;
            return find(record.scope(), record.key()).orElseThrow();
        } catch (SQLException e) { throw new IllegalArgumentException("Could not store idempotency record", e); }
    }

    private static void bindKey(java.sql.PreparedStatement statement, IdempotencyScope scope, IdempotencyKey key) throws SQLException {
        statement.setString(1, scope.domain());
        statement.setString(2, scope.operation());
        statement.setString(3, scope.actor());
        statement.setString(4, key.value());
    }
}
