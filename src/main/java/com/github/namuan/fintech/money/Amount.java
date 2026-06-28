package com.github.namuan.fintech.money;

import java.math.BigDecimal;

public sealed interface Amount permits ScaledAmount, DecimalAmount {
    BigDecimal toBigDecimal();
    int scale();
}
