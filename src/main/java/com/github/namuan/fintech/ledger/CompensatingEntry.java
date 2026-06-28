package com.github.namuan.fintech.ledger;

public record CompensatingEntry(JournalEntry entry, String explanation) {}
