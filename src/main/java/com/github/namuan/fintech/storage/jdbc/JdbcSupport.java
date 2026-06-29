package com.github.namuan.fintech.storage.jdbc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.namuan.fintech.currency.AssetId;
import com.github.namuan.fintech.currency.CryptoAsset;
import com.github.namuan.fintech.currency.FiatCurrency;
import com.github.namuan.fintech.money.Money;
import com.github.namuan.fintech.money.MoneyParser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

final class JdbcSupport {
    static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};

    private JdbcSupport() {}

    static String toJson(Map<String, String> value) {
        try { return JSON.writeValueAsString(value == null ? Map.of() : value); }
        catch (Exception e) { throw new IllegalArgumentException("Could not serialize metadata", e); }
    }

    static Map<String, String> stringMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try { return JSON.readValue(json, STRING_MAP); }
        catch (Exception e) { throw new IllegalArgumentException("Could not deserialize metadata", e); }
    }

    static void setInstant(PreparedStatement statement, int index, Instant value) throws SQLException {
        if (value == null) statement.setTimestamp(index, null);
        else statement.setTimestamp(index, Timestamp.from(value));
    }

    static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    static Optional<Instant> optionalInstant(ResultSet rs, String column) throws SQLException {
        return Optional.ofNullable(instant(rs, column));
    }

    static void setMoney(PreparedStatement statement, int startIndex, Money money) throws SQLException {
        statement.setString(startIndex, money.decimalValue().toPlainString());
        AssetId asset = money.asset();
        if (asset instanceof FiatCurrency fiat) {
            statement.setString(startIndex + 1, "FIAT");
            statement.setString(startIndex + 2, fiat.code());
            statement.setInt(startIndex + 3, fiat.scale());
            statement.setString(startIndex + 4, fiat.displayName());
            statement.setString(startIndex + 5, null);
            statement.setString(startIndex + 6, null);
            statement.setString(startIndex + 7, null);
        } else if (asset instanceof CryptoAsset crypto) {
            statement.setString(startIndex + 1, "CRYPTO");
            statement.setString(startIndex + 2, crypto.code());
            statement.setInt(startIndex + 3, crypto.scale());
            statement.setString(startIndex + 4, crypto.displayName());
            statement.setString(startIndex + 5, crypto.network());
            statement.setString(startIndex + 6, crypto.contractAddress().orElse(null));
            statement.setString(startIndex + 7, crypto.symbol());
        } else {
            throw new IllegalArgumentException("Unsupported asset type: " + asset.getClass().getName());
        }
    }

    static Money money(ResultSet rs, String prefix) throws SQLException {
        String amount = rs.getString(prefix + "amount");
        return MoneyParser.decimalString(amount, asset(rs, prefix));
    }

    static AssetId asset(ResultSet rs, String prefix) throws SQLException {
        String type = rs.getString(prefix + "asset_type");
        if ("FIAT".equals(type)) {
            return new FiatCurrency(rs.getString(prefix + "asset_code"), rs.getInt(prefix + "asset_scale"), rs.getString(prefix + "asset_display_name"));
        }
        if ("CRYPTO".equals(type)) {
            return new CryptoAsset(
                    rs.getString(prefix + "asset_network"),
                    Optional.ofNullable(rs.getString(prefix + "asset_contract_address")),
                    rs.getString(prefix + "asset_symbol"),
                    rs.getInt(prefix + "asset_scale"),
                    rs.getString(prefix + "asset_display_name")
            );
        }
        throw new IllegalArgumentException("Unsupported stored asset type: " + type);
    }

    static void rollbackQuietly(Connection connection) {
        try { connection.rollback(); } catch (SQLException ignored) { }
    }
}
