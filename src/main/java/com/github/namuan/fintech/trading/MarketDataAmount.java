package com.github.namuan.fintech.trading;
import java.math.BigDecimal;
public record MarketDataAmount(BigDecimal value, int scale, boolean performanceSensitive) {}
