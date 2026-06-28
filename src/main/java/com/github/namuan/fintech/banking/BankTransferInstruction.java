package com.github.namuan.fintech.banking;
import com.github.namuan.fintech.money.Money;
public record BankTransferInstruction(String id, String originatorAccount, String beneficiaryAccount, Money amount, String rail) {}
