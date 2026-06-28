package com.github.namuan.fintech.ledger;

import com.github.namuan.fintech.time.FinancialTimestampSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record JournalEntry(
        JournalEntryId id,
        List<Posting> postings,
        FinancialTimestampSet timestamps,
        String reason,
        Map<String, String> metadata
) {
    public JournalEntry {
        Objects.requireNonNull(id, "id");
        postings = List.copyOf(Objects.requireNonNull(postings, "postings"));
        Objects.requireNonNull(timestamps, "timestamps");
        reason = reason == null ? "" : reason;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (postings.size() < 2) throw new IllegalArgumentException("journal entry needs at least two postings");
        LedgerInvariantChecker.requireBalanced(postings);
    }
}
