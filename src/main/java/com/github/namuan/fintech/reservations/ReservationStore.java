package com.github.namuan.fintech.reservations;
import com.github.namuan.fintech.ledger.AccountId;
import java.util.List;
import java.util.Optional;
public interface ReservationStore { void save(Reservation reservation); Optional<Reservation> find(ReservationId id); List<Reservation> heldFor(AccountId accountId); }
