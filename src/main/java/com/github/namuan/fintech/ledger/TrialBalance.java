package com.github.namuan.fintech.ledger;

import java.util.Map;

public record TrialBalance(Map<AccountId, String> balances) {}
