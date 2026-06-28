package com.github.namuan.fintech.ledger;

import java.util.UUID;

public record JournalEntryId(String value) {
    public JournalEntryId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("journal entry id is required");
    }
    public static JournalEntryId random() { return new JournalEntryId(UUID.randomUUID().toString()); }
}
