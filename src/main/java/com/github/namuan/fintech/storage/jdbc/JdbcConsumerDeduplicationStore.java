package com.github.namuan.fintech.storage.jdbc;

import com.github.namuan.fintech.outbox.ConsumerDeduplicationStore;
import com.github.namuan.fintech.outbox.PublishedEventId;

import javax.sql.DataSource;
import java.sql.SQLException;

public final class JdbcConsumerDeduplicationStore implements ConsumerDeduplicationStore {
    private final DataSource dataSource;
    public JdbcConsumerDeduplicationStore(DataSource dataSource) { this.dataSource = dataSource; }
    @Override public boolean markIfFirst(PublishedEventId id) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("insert into fintech_consumed_events(id,consumed_at) values(?,now()) on conflict do nothing")) { statement.setString(1, id.value()); return statement.executeUpdate() == 1; }
        catch (SQLException e) { throw new IllegalArgumentException("Could not mark consumed event", e); }
    }
}
