package com.github.namuan.fintech.storage.jdbc;

import com.github.namuan.fintech.ledger.*;
import com.github.namuan.fintech.time.FinancialTimestampSet;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JdbcLedgerStore implements LedgerStore {
    private final DataSource dataSource;

    public JdbcLedgerStore(DataSource dataSource) { this.dataSource = dataSource; }

    @Override public void append(JournalEntry entry) {
        try (Connection connection = dataSource.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.prepareStatement("insert into fintech_ledger_entries(id,value_time,booking_time,settlement_time,reason,metadata_json) values(?,?,?,?,?,?::jsonb)")) {
                    statement.setString(1, entry.id().value());
                    JdbcSupport.setInstant(statement, 2, entry.timestamps().valueTime());
                    JdbcSupport.setInstant(statement, 3, entry.timestamps().bookingTime());
                    JdbcSupport.setInstant(statement, 4, entry.timestamps().settlementTime().orElse(null));
                    statement.setString(5, entry.reason());
                    statement.setString(6, JdbcSupport.toJson(entry.metadata()));
                    statement.executeUpdate();
                }
                int ordinal = 0;
                for (Posting posting : entry.postings()) {
                    try (var statement = connection.prepareStatement("insert into fintech_ledger_postings(entry_id,ordinal,account_id,side,amount,asset_type,asset_code,asset_scale,asset_display_name,asset_network,asset_contract_address,asset_symbol,memo) values(?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                        statement.setString(1, entry.id().value());
                        statement.setInt(2, ordinal++);
                        statement.setString(3, posting.accountId().value());
                        statement.setString(4, posting.side().name());
                        JdbcSupport.setMoney(statement, 5, posting.amount());
                        statement.setString(13, posting.memo());
                        statement.executeUpdate();
                    }
                }
                connection.commit();
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException | RuntimeException e) {
                JdbcSupport.rollbackQuietly(connection);
                connection.setAutoCommit(previousAutoCommit);
                throw e;
            }
        } catch (SQLException e) { throw new IllegalArgumentException("Could not append ledger entry", e); }
    }

    @Override public Optional<JournalEntry> find(JournalEntryId id) {
        try (Connection connection = dataSource.getConnection()) { return find(connection, id); }
        catch (SQLException e) { throw new IllegalArgumentException("Could not load ledger entry", e); }
    }

    @Override public List<JournalEntry> all() {
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement("select id from fintech_ledger_entries order by booking_time,id"); var rs = statement.executeQuery()) {
            List<JournalEntry> entries = new ArrayList<>();
            while (rs.next()) entries.add(find(connection, new JournalEntryId(rs.getString("id"))).orElseThrow());
            return List.copyOf(entries);
        } catch (SQLException e) { throw new IllegalArgumentException("Could not load ledger entries", e); }
    }

    private Optional<JournalEntry> find(Connection connection, JournalEntryId id) throws SQLException {
        try (var statement = connection.prepareStatement("select * from fintech_ledger_entries where id=?")) {
            statement.setString(1, id.value());
            try (var rs = statement.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                FinancialTimestampSet timestamps = new FinancialTimestampSet(
                        JdbcSupport.instant(rs, "value_time"),
                        JdbcSupport.instant(rs, "booking_time"),
                        JdbcSupport.optionalInstant(rs, "settlement_time")
                );
                String reason = rs.getString("reason");
                Map<String, String> metadata = JdbcSupport.stringMap(rs.getString("metadata_json"));
                return Optional.of(new JournalEntry(id, postings(connection, id), timestamps, reason, metadata));
            }
        }
    }

    private List<Posting> postings(Connection connection, JournalEntryId id) throws SQLException {
        try (var statement = connection.prepareStatement("select account_id,side,amount,asset_type,asset_code,asset_scale,asset_display_name,asset_network,asset_contract_address,asset_symbol,memo from fintech_ledger_postings where entry_id=? order by ordinal")) {
            statement.setString(1, id.value());
            try (var rs = statement.executeQuery()) {
                List<Posting> postings = new ArrayList<>();
                while (rs.next()) postings.add(new Posting(new AccountId(rs.getString("account_id")), DebitCredit.valueOf(rs.getString("side")), JdbcSupport.money(rs, ""), rs.getString("memo")));
                return postings;
            }
        }
    }
}
