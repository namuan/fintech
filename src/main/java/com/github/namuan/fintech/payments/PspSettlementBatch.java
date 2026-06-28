package com.github.namuan.fintech.payments;
import com.github.namuan.fintech.money.Money; import java.time.Instant; import java.util.List;
public record PspSettlementBatch(String id, String providerId, Money total, List<String> paymentIds, Instant settledAt) { public PspSettlementBatch { paymentIds = List.copyOf(paymentIds); } }
