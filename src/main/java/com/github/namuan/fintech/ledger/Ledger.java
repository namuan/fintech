package com.github.namuan.fintech.ledger;

import com.github.namuan.fintech.money.DecimalAmount;
import com.github.namuan.fintech.money.Money;
import java.math.BigDecimal;

public final class Ledger {
    private final LedgerStore store;

    public Ledger(LedgerStore store) { this.store = store; }

    public void post(JournalEntry entry) { store.append(entry); }

    public Money balance(Account account) {
        BigDecimal value = BigDecimal.ZERO;
        for (JournalEntry entry : store.all()) {
            for (Posting posting : entry.postings()) {
                if (posting.accountId().equals(account.id())) {
                    int sign = normalBalanceSide(account.type()) == posting.side() ? 1 : -1;
                    value = value.add(posting.amount().decimalValue().multiply(BigDecimal.valueOf(sign)));
                }
            }
        }
        return new Money(new DecimalAmount(value), account.asset());
    }

    private DebitCredit normalBalanceSide(AccountType type) {
        return switch (type) {
            case ASSET, EXPENSE -> DebitCredit.DEBIT;
            case LIABILITY, EQUITY, REVENUE -> DebitCredit.CREDIT;
        };
    }
}
