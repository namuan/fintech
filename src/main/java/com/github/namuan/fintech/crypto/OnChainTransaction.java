package com.github.namuan.fintech.crypto;
import com.github.namuan.fintech.money.Money; import java.time.Instant;
public record OnChainTransaction(String txHash, WalletAddress from, WalletAddress to, Money amount, int confirmations, Instant observedAt) {}
