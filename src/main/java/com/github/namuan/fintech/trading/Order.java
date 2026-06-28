package com.github.namuan.fintech.trading;
import com.github.namuan.fintech.money.Money; import java.time.Instant;
public record Order(String id, Instrument instrument, OrderSide side, OrderType type, Money quantity, Money limitPrice, Instant createdAt) {}
