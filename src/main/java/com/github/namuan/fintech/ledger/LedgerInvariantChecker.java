package com.github.namuan.fintech.ledger;

import com.github.namuan.fintech.currency.AssetId;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LedgerInvariantChecker {
    private LedgerInvariantChecker() {}

    public static void requireBalanced(List<Posting> postings) {
        Map<AssetId, BigDecimal> debits = new HashMap<>();
        Map<AssetId, BigDecimal> credits = new HashMap<>();
        for (Posting p : postings) {
            var target = p.side() == DebitCredit.DEBIT ? debits : credits;
            target.merge(p.amount().asset(), p.amount().decimalValue(), BigDecimal::add);
        }
        if (!debits.equals(credits)) throw new IllegalArgumentException("journal entry is not balanced: debits=" + debits + " credits=" + credits);
    }
}
