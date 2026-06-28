package com.github.namuan.fintech.ledger;

public record CorrectionLink(JournalEntryId original, JournalEntryId correction, CorrectionType type) {}
