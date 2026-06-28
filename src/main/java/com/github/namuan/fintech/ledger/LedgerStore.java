package com.github.namuan.fintech.ledger;

import java.util.List;
import java.util.Optional;

public interface LedgerStore {
    void append(JournalEntry entry);
    Optional<JournalEntry> find(JournalEntryId id);
    List<JournalEntry> all();
}
