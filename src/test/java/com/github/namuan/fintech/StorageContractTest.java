package com.github.namuan.fintech;

import com.github.namuan.fintech.audit.AuditActor;
import com.github.namuan.fintech.audit.AuditEvent;
import com.github.namuan.fintech.audit.AuditReason;
import com.github.namuan.fintech.audit.AuditTrailStore;
import com.github.namuan.fintech.audit.InMemoryAuditTrailStore;
import com.github.namuan.fintech.currency.FiatCurrency;
import com.github.namuan.fintech.idempotency.IdempotencyBarrier;
import com.github.namuan.fintech.idempotency.IdempotencyKey;
import com.github.namuan.fintech.idempotency.IdempotencyRecord;
import com.github.namuan.fintech.idempotency.IdempotencyScope;
import com.github.namuan.fintech.idempotency.IdempotencyStore;
import com.github.namuan.fintech.idempotency.InMemoryIdempotencyStore;
import com.github.namuan.fintech.ledger.Account;
import com.github.namuan.fintech.ledger.AccountId;
import com.github.namuan.fintech.ledger.AccountType;
import com.github.namuan.fintech.ledger.DebitCredit;
import com.github.namuan.fintech.ledger.InMemoryLedgerStore;
import com.github.namuan.fintech.ledger.JournalEntry;
import com.github.namuan.fintech.ledger.JournalEntryId;
import com.github.namuan.fintech.ledger.Ledger;
import com.github.namuan.fintech.ledger.LedgerStore;
import com.github.namuan.fintech.ledger.Posting;
import com.github.namuan.fintech.money.Money;
import com.github.namuan.fintech.outbox.ConsumerDeduplicationStore;
import com.github.namuan.fintech.outbox.InMemoryConsumerDeduplicationStore;
import com.github.namuan.fintech.outbox.InMemoryOutboxStore;
import com.github.namuan.fintech.outbox.OutboxEvent;
import com.github.namuan.fintech.outbox.OutboxStore;
import com.github.namuan.fintech.outbox.PublishedEventId;
import com.github.namuan.fintech.reservations.InMemoryReservationStore;
import com.github.namuan.fintech.reservations.Reservation;
import com.github.namuan.fintech.reservations.ReservationId;
import com.github.namuan.fintech.reservations.ReservationRelease;
import com.github.namuan.fintech.reservations.ReservationService;
import com.github.namuan.fintech.reservations.ReservationSettlement;
import com.github.namuan.fintech.reservations.ReservationStatus;
import com.github.namuan.fintech.reservations.ReservationStore;
import com.github.namuan.fintech.time.FinancialTimestampSet;
import com.github.namuan.fintech.webhooks.InMemoryRawWebhookStore;
import com.github.namuan.fintech.webhooks.RawWebhookStore;
import com.github.namuan.fintech.webhooks.WebhookEnvelope;
import com.github.namuan.fintech.workflow.InMemoryWorkflowStore;
import com.github.namuan.fintech.workflow.WorkflowState;
import com.github.namuan.fintech.workflow.WorkflowStore;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageContractTest {
    @Nested
    class InMemoryLedgerStoreContract implements LedgerStoreContract {
        @Override public LedgerStore newStore() { return new InMemoryLedgerStore(); }
    }

    @Nested
    class InMemoryIdempotencyStoreContract implements IdempotencyStoreContract {
        @Override public IdempotencyStore newStore() { return new InMemoryIdempotencyStore(); }
    }

    @Nested
    class InMemoryReservationStoreContract implements ReservationStoreContract {
        @Override public ReservationStore newStore() { return new InMemoryReservationStore(); }
    }

    @Nested
    class InMemoryWorkflowStoreContract implements WorkflowStoreContract {
        @Override public WorkflowStore newStore() { return new InMemoryWorkflowStore(); }
    }

    @Nested
    class InMemoryRawWebhookStoreContract implements RawWebhookStoreContract {
        @Override public RawWebhookStore newStore() { return new InMemoryRawWebhookStore(); }
    }

    @Nested
    class InMemoryOutboxStoreContract implements OutboxStoreContract {
        @Override public OutboxStore newStore() { return new InMemoryOutboxStore(); }
    }

    @Nested
    class InMemoryConsumerDeduplicationStoreContract implements ConsumerDeduplicationStoreContract {
        @Override public ConsumerDeduplicationStore newStore() { return new InMemoryConsumerDeduplicationStore(); }
    }

    @Nested
    class InMemoryAuditTrailStoreContract implements AuditTrailStoreContract {
        @Override public AuditTrailStore newStore() { return new InMemoryAuditTrailStore(); }
    }
}

interface LedgerStoreContract {
    FiatCurrency USD = new FiatCurrency("USD", 2, "US Dollar");
    Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    LedgerStore newStore();

    @Test
    default void appendFindAndAllRoundTripEntryExactly() {
        LedgerStore store = newStore();
        JournalEntry entry = ledgerEntry(new JournalEntryId("entry-1"), Money.decimal("10.0", USD), Money.decimal("10.00", USD));

        store.append(entry);

        assertEquals(entry, store.find(entry.id()).orElseThrow());
        assertTrue(store.all().contains(entry));
    }

    @Test
    default void duplicateEntryIdIsRejectedAndOriginalSurvives() {
        LedgerStore store = newStore();
        JournalEntry original = ledgerEntry(new JournalEntryId("entry-duplicate"), Money.decimal("10.00", USD), Money.decimal("10.00", USD));
        JournalEntry duplicate = new JournalEntry(
                original.id(),
                List.of(
                        new Posting(new AccountId("cash"), DebitCredit.DEBIT, Money.decimal("11.00", USD), "cash"),
                        new Posting(new AccountId("user"), DebitCredit.CREDIT, Money.decimal("11.00", USD), "user")
                ),
                FinancialTimestampSet.bookedNow(NOW),
                "duplicate",
                Map.of()
        );

        store.append(original);

        assertThrows(IllegalArgumentException.class, () -> store.append(duplicate));
        assertEquals(original, store.find(original.id()).orElseThrow());
    }

    private static JournalEntry ledgerEntry(JournalEntryId id, Money debit, Money credit) {
        return new JournalEntry(
                id,
                List.of(
                        new Posting(new AccountId("cash"), DebitCredit.DEBIT, debit, "cash"),
                        new Posting(new AccountId("user"), DebitCredit.CREDIT, credit, "user")
                ),
                FinancialTimestampSet.bookedNow(NOW),
                "contract",
                Map.of("contract", "ledger")
        );
    }
}

interface IdempotencyStoreContract {
    IdempotencyStore newStore();

    @Test
    default void putIfAbsentKeepsFirstRecord() {
        IdempotencyStore store = newStore();
        IdempotencyScope scope = new IdempotencyScope("withdraw", "actor", "payments");
        IdempotencyKey key = new IdempotencyKey("key-1");
        IdempotencyRecord first = new IdempotencyRecord(scope, key, "payload", "result-1");
        IdempotencyRecord second = new IdempotencyRecord(scope, key, "payload", "result-2");

        assertEquals(null, store.putIfAbsent(first));
        assertEquals(first, store.putIfAbsent(second));
        assertEquals(first, store.find(scope, key).orElseThrow());
    }

    @Test
    default void barrierCollapsesDuplicateEffectsAndRejectsChangedPayload() {
        IdempotencyBarrier barrier = new IdempotencyBarrier(newStore());
        IdempotencyScope scope = new IdempotencyScope("capture", "actor", "payments");
        IdempotencyKey key = new IdempotencyKey("key-2");
        AtomicInteger effects = new AtomicInteger();

        var first = barrier.execute(scope, key, "payload", () -> "effect-" + effects.incrementAndGet());
        var second = barrier.execute(scope, key, "payload", () -> "effect-" + effects.incrementAndGet());

        assertFalse(first.replayed());
        assertTrue(second.replayed());
        assertEquals("effect-1", second.value());
        assertEquals(1, effects.get());
        assertThrows(IllegalArgumentException.class, () -> barrier.execute(scope, key, "changed", () -> "bad"));
    }
}

interface ReservationStoreContract {
    FiatCurrency USD = new FiatCurrency("USD", 2, "US Dollar");
    Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    ReservationStore newStore();

    @Test
    default void heldForReturnsOnlyHeldReservationsForAccount() {
        ReservationStore store = newStore();
        AccountId account = new AccountId("account");
        AccountId other = new AccountId("other");
        Reservation held = new Reservation(new ReservationId("held"), account, Money.decimal("10.00", USD), ReservationStatus.HELD, NOW);

        store.save(held);
        store.save(new Reservation(new ReservationId("settled"), account, Money.decimal("10.00", USD), ReservationStatus.SETTLED, NOW));
        store.save(new Reservation(new ReservationId("other"), other, Money.decimal("10.00", USD), ReservationStatus.HELD, NOW));

        assertEquals(List.of(held), store.heldFor(account));
    }

    @Test
    default void reservationResolutionIsSingleUse() {
        ReservationStore store = newStore();
        Account cash = new Account(new AccountId("cash"), "Cash", AccountType.ASSET, USD);
        Account user = new Account(new AccountId("user"), "User", AccountType.LIABILITY, USD);
        Ledger ledger = fundedLedger(cash, user, Money.decimal("100.00", USD));
        ReservationService service = new ReservationService(store, ledger);
        Reservation reservation = service.reserve(user, Money.decimal("10.00", USD));

        service.settle(new ReservationSettlement(reservation.id(), Money.decimal("10.00", USD)));

        assertThrows(IllegalStateException.class, () -> service.release(new ReservationRelease(reservation.id(), "already settled")));
    }

    @Test
    default void concurrentReservationsCannotOverReserve() throws Exception {
        ReservationStore store = newStore();
        Account cash = new Account(new AccountId("cash"), "Cash", AccountType.ASSET, USD);
        Account user = new Account(new AccountId("user"), "User", AccountType.LIABILITY, USD);
        ReservationService service = new ReservationService(store, fundedLedger(cash, user, Money.decimal("100.00", USD)));
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> reserve = () -> {
            start.await();
            try {
                service.reserve(user, Money.decimal("75.00", USD));
                return true;
            } catch (IllegalStateException expected) {
                return false;
            }
        };

        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(reserve);
            Future<Boolean> second = executor.submit(reserve);
            start.countDown();

            int successes = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);

            assertEquals(1, successes);
            assertEquals(1, store.heldFor(user.id()).size());
        } finally {
            executor.shutdownNow();
        }
    }

    private static Ledger fundedLedger(Account cash, Account user, Money amount) {
        Ledger ledger = new Ledger(new InMemoryLedgerStore());
        ledger.post(new JournalEntry(
                JournalEntryId.random(),
                List.of(
                        new Posting(cash.id(), DebitCredit.DEBIT, amount, "cash"),
                        new Posting(user.id(), DebitCredit.CREDIT, amount, "user")
                ),
                FinancialTimestampSet.bookedNow(NOW),
                "fund",
                Map.of()
        ));
        return ledger;
    }
}

interface WorkflowStoreContract {
    WorkflowStore newStore();

    @Test
    default void saveReplacesOnlyLatestStateForWorkflow() {
        WorkflowStore store = newStore();
        WorkflowState first = new WorkflowState("workflow", "reserve", false, Map.of("reservation", "r1"));
        WorkflowState second = new WorkflowState("workflow", "settle", true, Map.of("reservation", "r1", "settled", "true"));

        store.save(first);
        store.save(second);

        assertEquals(second, store.load("workflow").orElseThrow());
    }
}

interface RawWebhookStoreContract {
    Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    RawWebhookStore newStore();

    @Test
    default void appendPreservesExactRawPayloadBytes() {
        RawWebhookStore store = newStore();
        byte[] payload = new byte[] {0, 1, 2, 3, -1};
        WebhookEnvelope envelope = new WebhookEnvelope("evt-1", "provider", NOW, Map.of("signature", "abc"), payload);
        payload[0] = 99;

        store.append(envelope);

        assertArrayEquals(new byte[] {0, 1, 2, 3, -1}, store.find("evt-1").orElseThrow().rawBody());
    }

    @Test
    default void duplicateWebhookIdKeepsFirstPayload() {
        RawWebhookStore store = newStore();
        WebhookEnvelope first = new WebhookEnvelope("evt-duplicate", "provider", NOW, Map.of(), "first".getBytes());
        WebhookEnvelope second = new WebhookEnvelope("evt-duplicate", "provider", NOW, Map.of(), "second".getBytes());

        store.append(first);
        store.append(second);

        assertArrayEquals("first".getBytes(), store.find("evt-duplicate").orElseThrow().rawBody());
        assertEquals(1, store.all().size());
    }
}

interface OutboxStoreContract {
    Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    OutboxStore newStore();

    @Test
    default void appendedEventsRemainPendingUntilMarkedPublished() {
        OutboxStore store = newStore();
        OutboxEvent event = new OutboxEvent(new PublishedEventId("event-1"), "payments", "{}", NOW, Map.of());

        store.append(event);

        assertTrue(store.pending().contains(event));
        store.markPublished(event.id());
        assertFalse(store.pending().contains(event));
    }

    @Test
    default void duplicateOutboxIdCannotSilentlyOverwritePayload() {
        OutboxStore store = newStore();
        PublishedEventId id = new PublishedEventId("event-duplicate");
        OutboxEvent first = new OutboxEvent(id, "payments", "first", NOW, Map.of());
        OutboxEvent second = new OutboxEvent(id, "payments", "second", NOW, Map.of());

        store.append(first);

        assertThrows(IllegalArgumentException.class, () -> store.append(second));
        assertTrue(store.pending().contains(first));
        assertFalse(store.pending().contains(second));
    }
}

interface ConsumerDeduplicationStoreContract {
    ConsumerDeduplicationStore newStore();

    @Test
    default void markIfFirstReturnsTrueOnlyOncePerEventId() {
        ConsumerDeduplicationStore store = newStore();
        PublishedEventId id = new PublishedEventId("event-1");

        assertTrue(store.markIfFirst(id));
        assertFalse(store.markIfFirst(id));
        assertTrue(store.markIfFirst(new PublishedEventId("event-2")));
    }
}

interface AuditTrailStoreContract {
    Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    AuditTrailStore newStore();

    @Test
    default void appendPreservesAuditEventsAndOrderMetadata() {
        AuditTrailStore store = newStore();
        AuditEvent first = auditEvent("1", NOW);
        AuditEvent second = auditEvent("2", NOW.plusSeconds(1));

        store.append(first);
        store.append(second);

        assertEquals(List.of(first, second), store.all());
        assertNotEquals(first.id(), second.id());
    }

    private static AuditEvent auditEvent(String id, Instant occurredAt) {
        return new AuditEvent(
                "audit-" + id,
                occurredAt,
                new AuditActor("user", "alice"),
                "change",
                new AuditReason("test", "storage contract"),
                Map.of("sequence", id)
        );
    }
}
