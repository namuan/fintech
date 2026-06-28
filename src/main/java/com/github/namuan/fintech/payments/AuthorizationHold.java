package com.github.namuan.fintech.payments;
import com.github.namuan.fintech.money.Money; import java.time.Instant;
public record AuthorizationHold(String id, String paymentIntentId, Money amount, Instant authorizedAt) {}
