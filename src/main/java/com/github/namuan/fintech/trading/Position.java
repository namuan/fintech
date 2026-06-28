package com.github.namuan.fintech.trading;
import com.github.namuan.fintech.money.Money;
public record Position(String accountId, Instrument instrument, Money quantity) {}
