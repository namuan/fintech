package com.github.namuan.fintech.crypto;
import com.github.namuan.fintech.money.Money;
public record NetworkFeeEstimate(Money estimatedFee, String source) {}
