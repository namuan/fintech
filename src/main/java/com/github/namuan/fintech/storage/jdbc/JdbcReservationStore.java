package com.github.namuan.fintech.storage.jdbc;

import com.github.namuan.fintech.ledger.AccountId;
import com.github.namuan.fintech.reservations.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcReservationStore implements ReservationStore {
    private final DataSource dataSource;
    public JdbcReservationStore(DataSource dataSource) { this.dataSource = dataSource; }

    @Override public void save(Reservation reservation) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("insert into fintech_reservations(id,account_id,status,created_at,amount,asset_type,asset_code,asset_scale,asset_display_name,asset_network,asset_contract_address,asset_symbol) values(?,?,?,?,?,?,?,?,?,?,?,?) on conflict(id) do update set status=excluded.status")) {
            statement.setString(1, reservation.id().value());
            statement.setString(2, reservation.accountId().value());
            statement.setString(3, reservation.status().name());
            JdbcSupport.setInstant(statement, 4, reservation.createdAt());
            JdbcSupport.setMoney(statement, 5, reservation.estimatedAmount());
            statement.executeUpdate();
        } catch (SQLException e) { throw new IllegalArgumentException("Could not save reservation", e); }
    }

    @Override public Optional<Reservation> find(ReservationId id) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("select * from fintech_reservations where id=?")) {
            statement.setString(1, id.value());
            try (var rs = statement.executeQuery()) { return rs.next() ? Optional.of(reservation(rs)) : Optional.empty(); }
        } catch (SQLException e) { throw new IllegalArgumentException("Could not load reservation", e); }
    }

    @Override public List<Reservation> heldFor(AccountId accountId) {
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement("select * from fintech_reservations where account_id=? and status='HELD' order by created_at,id")) {
            statement.setString(1, accountId.value());
            try (var rs = statement.executeQuery()) {
                List<Reservation> reservations = new ArrayList<>();
                while (rs.next()) reservations.add(reservation(rs));
                return reservations;
            }
        } catch (SQLException e) { throw new IllegalArgumentException("Could not load held reservations", e); }
    }

    private static Reservation reservation(java.sql.ResultSet rs) throws SQLException {
        return new Reservation(new ReservationId(rs.getString("id")), new AccountId(rs.getString("account_id")), JdbcSupport.money(rs, ""), ReservationStatus.valueOf(rs.getString("status")), JdbcSupport.instant(rs, "created_at"));
    }
}
