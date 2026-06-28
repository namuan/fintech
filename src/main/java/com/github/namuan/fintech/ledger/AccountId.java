package com.github.namuan.fintech.ledger;

import java.util.UUID;

public record AccountId(String value) {
    public AccountId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("account id is required");
    }
    public static AccountId random() { return new AccountId(UUID.randomUUID().toString()); }
}
