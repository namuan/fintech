package com.github.namuan.fintech.banking;
public record AchDebitFlow(BankTransferInstruction instruction, BalanceCheckObservation balanceObservation, BankSettlementEvent settlement) {}
