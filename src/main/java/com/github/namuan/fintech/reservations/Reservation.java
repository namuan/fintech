package com.github.namuan.fintech.reservations;

import com.github.namuan.fintech.ledger.AccountId;
import com.github.namuan.fintech.money.Money;
import java.time.Instant;

public record Reservation(ReservationId id, AccountId accountId, Money estimatedAmount, ReservationStatus status, Instant createdAt) {}
