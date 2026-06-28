package com.github.namuan.fintech.ledger;

import com.github.namuan.fintech.time.FinancialTimestampSet;
import java.util.List;
import java.util.Map;

public final class ReversalEntry {
    private ReversalEntry() {}

    public static JournalEntry reverse(JournalEntry original, FinancialTimestampSet timestamps, String reason) {
        List<Posting> reversed = original.postings().stream()
                .map(p -> new Posting(p.accountId(), p.side() == DebitCredit.DEBIT ? DebitCredit.CREDIT : DebitCredit.DEBIT, p.amount(), "reversal of " + original.id().value()))
                .toList();
        return new JournalEntry(JournalEntryId.random(), reversed, timestamps, reason, Map.of("reverses", original.id().value()));
    }
}
