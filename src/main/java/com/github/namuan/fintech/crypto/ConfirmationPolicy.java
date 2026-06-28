package com.github.namuan.fintech.crypto;
public record ConfirmationPolicy(int requiredConfirmations) { public boolean finalEnough(OnChainTransaction tx){ return tx.confirmations() >= requiredConfirmations; } }
