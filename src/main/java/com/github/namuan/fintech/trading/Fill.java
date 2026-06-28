package com.github.namuan.fintech.trading;
import com.github.namuan.fintech.money.Money; import java.time.Instant;
public record Fill(String id, String orderId, Money quantity, Money price, Instant filledAt) {}
