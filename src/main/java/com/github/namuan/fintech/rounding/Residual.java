package com.github.namuan.fintech.rounding;

import com.github.namuan.fintech.money.Money;

public record Residual(Money value, String reason) {}
