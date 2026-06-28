package com.github.namuan.fintech.reservations;
import com.github.namuan.fintech.money.Money;
public record ReservationSettlement(ReservationId reservationId, Money actualAmount) {}
