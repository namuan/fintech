package com.github.namuan.fintech.ledger;

import com.github.namuan.fintech.money.Money;
import java.util.Objects;

public record Posting(AccountId accountId, DebitCredit side, Money amount, String memo) {
    public Posting {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(amount, "amount");
        memo = memo == null ? "" : memo;
        if (amount.isNegative()) throw new IllegalArgumentException("posting amount must be non-negative; use debit/credit side, not negative amounts");
    }
}
