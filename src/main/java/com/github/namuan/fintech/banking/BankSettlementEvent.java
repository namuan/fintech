package com.github.namuan.fintech.banking;
import java.time.Instant;
public record BankSettlementEvent(String instructionId, BankSettlementStatus status, Instant settledAt, String providerReference) {}
