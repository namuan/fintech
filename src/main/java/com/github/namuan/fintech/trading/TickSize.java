package com.github.namuan.fintech.trading;
import java.math.BigDecimal;
public record TickSize(BigDecimal value) { public boolean validPrice(BigDecimal price){ return price.remainder(value).compareTo(BigDecimal.ZERO) == 0; } }
