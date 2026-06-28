package com.github.namuan.fintech;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.namuan.fintech.currency.FiatCurrency;
import com.github.namuan.fintech.idempotency.*;
import com.github.namuan.fintech.ledger.*;
import com.github.namuan.fintech.money.Money;
import com.github.namuan.fintech.reservations.*;
import com.github.namuan.fintech.rounding.RoundingContext;
import com.github.namuan.fintech.rounding.RoundingPolicy;
import com.github.namuan.fintech.serialization.MoneyJson;
import com.github.namuan.fintech.time.FinancialTimestampSet;
import com.github.namuan.fintech.workflow.*;
import org.junit.jupiter.api.Test;

import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CoreLibraryTest {
    private final FiatCurrency usd = new FiatCurrency("USD", 2, "US Dollar");

    @Test
    void forbidsCrossCurrencyArithmetic() {
        Money usdMoney = Money.decimal("10.00", usd);
        Money eurMoney = Money.decimal("10.00", new FiatCurrency("EUR", 2, "Euro"));

        assertThrows(IllegalArgumentException.class, () -> usdMoney.plus(eurMoney));
    }

    @Test
    void roundsWithResidual() {
        var policy = new RoundingPolicy("display-usd", 2, RoundingMode.HALF_UP);
        var result = policy.apply(Money.decimal("1.005", usd), new RoundingContext("display", Map.of()));

        assertEquals("1.01", result.rounded().decimalValue().toPlainString());
        assertTrue(result.residual().isPresent());
    }

    @Test
    void serializesMoneyAsJsonStringAmount() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(MoneyJson.from(Money.decimal("12.34", usd)));

        assertTrue(json.contains("\"amount\":\"12.34\""));
        assertTrue(json.contains("\"asset\":\"USD\""));
    }

    @Test
    void postsBalancedLedgerEntryAndDerivesBalance() {
        Account cash = new Account(new AccountId("cash"), "Cash", AccountType.ASSET, usd);
        Account user = new Account(new AccountId("user"), "User Liability", AccountType.LIABILITY, usd);
        Ledger ledger = new Ledger(new InMemoryLedgerStore());

        ledger.post(new JournalEntry(
                JournalEntryId.random(),
                List.of(
                        new Posting(cash.id(), DebitCredit.DEBIT, Money.decimal("10.00", usd), "deposit cash"),
                        new Posting(user.id(), DebitCredit.CREDIT, Money.decimal("10.00", usd), "user balance")
                ),
                FinancialTimestampSet.bookedNow(Instant.now()),
                "deposit",
                Map.of()
        ));

        assertEquals("10.00", ledger.balance(cash).decimalValue().toPlainString());
        assertEquals("10.00", ledger.balance(user).decimalValue().toPlainString());
    }

    @Test
    void rejectsUnbalancedLedgerEntry() {
        Account cash = new Account(new AccountId("cash"), "Cash", AccountType.ASSET, usd);
        Account user = new Account(new AccountId("user"), "User Liability", AccountType.LIABILITY, usd);

        assertThrows(IllegalArgumentException.class, () -> new JournalEntry(
                JournalEntryId.random(),
                List.of(
                        new Posting(cash.id(), DebitCredit.DEBIT, Money.decimal("10.00", usd), "deposit cash"),
                        new Posting(user.id(), DebitCredit.CREDIT, Money.decimal("9.99", usd), "user balance")
                ),
                FinancialTimestampSet.bookedNow(Instant.now()),
                "bad deposit",
                Map.of()
        ));
    }

    @Test
    void idempotencyBarrierReplaysStoredResult() {
        IdempotencyBarrier barrier = new IdempotencyBarrier(new InMemoryIdempotencyStore());
        IdempotencyScope scope = new IdempotencyScope("withdraw", "user-1", "payments");
        IdempotencyKey key = new IdempotencyKey("k1");
        AtomicInteger calls = new AtomicInteger();

        var first = barrier.execute(scope, key, "payload", () -> "result-" + calls.incrementAndGet());
        var second = barrier.execute(scope, key, "payload", () -> "result-" + calls.incrementAndGet());

        assertFalse(first.replayed());
        assertTrue(second.replayed());
        assertEquals("result-1", second.value());
        assertEquals(1, calls.get());
    }

    @Test
    void reservationReducesAvailableBalanceAndCanResolve() {
        Account cash = new Account(new AccountId("cash"), "Cash", AccountType.ASSET, usd);
        Account user = new Account(new AccountId("user"), "User Liability", AccountType.LIABILITY, usd);
        Ledger ledger = new Ledger(new InMemoryLedgerStore());
        ledger.post(new JournalEntry(
                JournalEntryId.random(),
                List.of(
                        new Posting(cash.id(), DebitCredit.DEBIT, Money.decimal("25.00", usd), "cash"),
                        new Posting(user.id(), DebitCredit.CREDIT, Money.decimal("25.00", usd), "user")
                ),
                FinancialTimestampSet.bookedNow(Instant.now()),
                "deposit",
                Map.of()
        ));

        var service = new ReservationService(new InMemoryReservationStore(), ledger);
        Reservation reservation = service.reserve(user, Money.decimal("10.00", usd));
        Reservation settled = service.settle(new ReservationSettlement(reservation.id(), Money.decimal("9.50", usd)));

        assertEquals(ReservationStatus.HELD, reservation.status());
        assertEquals(ReservationStatus.SETTLED, settled.status());
    }

    @Test
    void workflowDriverPersistsCompletion() throws Exception {
        InMemoryWorkflowStore store = new InMemoryWorkflowStore();
        WorkflowDriver driver = new WorkflowDriver(store);
        MoneyWorkflow workflow = new MoneyWorkflow("sample", List.of(
                new NamedWorkflowStep("reserve", state -> new WorkflowState(state.workflowId(), "reserve", false, Map.of("reserved", "true"))),
                new NamedWorkflowStep("settle", state -> new WorkflowState(state.workflowId(), "settle", false, Map.of("settled", "true")))
        ));

        WorkflowState done = driver.run("wf-1", workflow);

        assertTrue(done.completed());
        assertEquals(Optional.of(done), store.load("wf-1"));
    }
}
