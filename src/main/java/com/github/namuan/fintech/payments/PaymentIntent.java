package com.github.namuan.fintech.payments;
import com.github.namuan.fintech.money.Money;
public record PaymentIntent(String id, Money amount, String providerId, PaymentStatus status) {}
