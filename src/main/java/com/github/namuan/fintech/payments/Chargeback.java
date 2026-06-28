package com.github.namuan.fintech.payments;
import com.github.namuan.fintech.money.Money; import java.time.Instant;
public record Chargeback(String id, String paymentIntentId, Money amount, Instant receivedAt, String reason) {}
