package com.github.namuan.fintech.reservations;

import com.github.namuan.fintech.money.Money;
import java.util.Collection;

public final class AvailableBalanceCalculator {
    public Money available(Money total, Collection<Reservation> reservations) {
        Money reserved = reservations.stream()
                .filter(r -> r.status() == ReservationStatus.HELD)
                .map(Reservation::estimatedAmount)
                .reduce(Money.minorUnits(0, total.asset()), Money::plus);
        return total.minus(reserved);
    }
}
