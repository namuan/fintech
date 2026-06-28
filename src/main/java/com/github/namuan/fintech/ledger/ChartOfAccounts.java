package com.github.namuan.fintech.ledger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ChartOfAccounts {
    private final Map<AccountId, Account> accounts = new ConcurrentHashMap<>();

    public void add(Account account) { accounts.put(account.id(), account); }
    public Optional<Account> find(AccountId id) { return Optional.ofNullable(accounts.get(id)); }
    public Account require(AccountId id) { return find(id).orElseThrow(() -> new IllegalArgumentException("Unknown account: " + id.value())); }
}
