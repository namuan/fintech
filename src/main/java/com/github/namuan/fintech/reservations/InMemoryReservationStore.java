package com.github.namuan.fintech.reservations;
import com.github.namuan.fintech.ledger.AccountId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
public final class InMemoryReservationStore implements ReservationStore {
    private final ConcurrentHashMap<ReservationId, Reservation> reservations = new ConcurrentHashMap<>();
    public void save(Reservation reservation) { reservations.put(reservation.id(), reservation); }
    public Optional<Reservation> find(ReservationId id) { return Optional.ofNullable(reservations.get(id)); }
    public List<Reservation> heldFor(AccountId accountId) { return reservations.values().stream().filter(r -> r.accountId().equals(accountId) && r.status() == ReservationStatus.HELD).toList(); }
}
