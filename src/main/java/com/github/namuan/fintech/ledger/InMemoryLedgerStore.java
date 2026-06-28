package com.github.namuan.fintech.ledger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryLedgerStore implements LedgerStore {
    private final Map<JournalEntryId, JournalEntry> entries = new ConcurrentHashMap<>();

    @Override public void append(JournalEntry entry) {
        JournalEntry previous = entries.putIfAbsent(entry.id(), entry);
        if (previous != null) throw new IllegalArgumentException("Journal entry already exists: " + entry.id().value());
    }

    @Override public Optional<JournalEntry> find(JournalEntryId id) { return Optional.ofNullable(entries.get(id)); }
    @Override public List<JournalEntry> all() { return List.copyOf(new ArrayList<>(entries.values())); }
}
