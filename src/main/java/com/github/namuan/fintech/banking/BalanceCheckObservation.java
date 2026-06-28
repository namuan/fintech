package com.github.namuan.fintech.banking;
import com.github.namuan.fintech.money.Money; import java.time.Instant;
public record BalanceCheckObservation(String accountRef, Money observedBalance, Instant observedAt, boolean authoritative) {}
