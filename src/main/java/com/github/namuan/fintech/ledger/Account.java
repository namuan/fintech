package com.github.namuan.fintech.ledger;

import com.github.namuan.fintech.currency.AssetId;
import java.util.Objects;

public record Account(AccountId id, String name, AccountType type, AssetId asset) {
    public Account {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(asset, "asset");
    }
}
