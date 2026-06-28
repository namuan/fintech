package com.github.namuan.fintech.rounding;

import com.github.namuan.fintech.money.Money;
import java.util.Optional;

public record RoundingResult(Money rounded, Optional<Residual> residual) {
    public static RoundingResult exact(Money rounded) { return new RoundingResult(rounded, Optional.empty()); }
}
