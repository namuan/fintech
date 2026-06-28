package com.github.namuan.fintech.payments;
public record CardDepositFlow(PaymentIntent intent, AuthorizationHold hold, Capture capture) {}
