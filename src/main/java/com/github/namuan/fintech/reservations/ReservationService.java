package com.github.namuan.fintech.reservations;

import com.github.namuan.fintech.ledger.Account;
import com.github.namuan.fintech.ledger.AccountId;
import com.github.namuan.fintech.ledger.Ledger;
import com.github.namuan.fintech.money.Money;
import java.time.Instant;

public final class ReservationService {
    private final ReservationStore store;
    private final Ledger ledger;
    private final AvailableBalanceCalculator calculator = new AvailableBalanceCalculator();

    public ReservationService(ReservationStore store, Ledger ledger) { this.store = store; this.ledger = ledger; }

    public synchronized Reservation reserve(Account account, Money amount) {
        amount.requireSameAsset(ledger.balance(account));
        Money available = calculator.available(ledger.balance(account), store.heldFor(account.id()));
        if (available.decimalValue().compareTo(amount.decimalValue()) < 0) throw new IllegalStateException("insufficient available balance");
        Reservation reservation = new Reservation(ReservationId.random(), account.id(), amount, ReservationStatus.HELD, Instant.now());
        store.save(reservation);
        return reservation;
    }

    public synchronized Reservation settle(ReservationSettlement settlement) { return transition(settlement.reservationId(), ReservationStatus.SETTLED); }
    public synchronized Reservation release(ReservationRelease release) { return transition(release.reservationId(), ReservationStatus.RELEASED); }
    private Reservation transition(ReservationId id, ReservationStatus status) {
        Reservation current = store.find(id).orElseThrow();
        if (current.status() != ReservationStatus.HELD) throw new IllegalStateException("reservation already resolved");
        Reservation updated = new Reservation(current.id(), current.accountId(), current.estimatedAmount(), status, current.createdAt());
        store.save(updated);
        return updated;
    }
}
