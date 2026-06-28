package com.github.namuan.fintech;

import com.github.namuan.fintech.currency.FiatCurrency;
import com.github.namuan.fintech.idempotency.IdempotencyBarrier;
import com.github.namuan.fintech.idempotency.IdempotencyKey;
import com.github.namuan.fintech.idempotency.IdempotencyScope;
import com.github.namuan.fintech.idempotency.InMemoryIdempotencyStore;
import com.github.namuan.fintech.ledger.Account;
import com.github.namuan.fintech.ledger.AccountId;
import com.github.namuan.fintech.ledger.AccountType;
import com.github.namuan.fintech.ledger.DebitCredit;
import com.github.namuan.fintech.ledger.InMemoryLedgerStore;
import com.github.namuan.fintech.ledger.JournalEntry;
import com.github.namuan.fintech.ledger.JournalEntryId;
import com.github.namuan.fintech.ledger.Ledger;
import com.github.namuan.fintech.ledger.Posting;
import com.github.namuan.fintech.ledger.ReversalEntry;
import com.github.namuan.fintech.money.Money;
import com.github.namuan.fintech.money.MoneyParser;
import com.github.namuan.fintech.reconciliation.MatchRule;
import com.github.namuan.fintech.reconciliation.ReconciliationBreak;
import com.github.namuan.fintech.reconciliation.ReconciliationJob;
import com.github.namuan.fintech.reconciliation.ReconciliationRecord;
import com.github.namuan.fintech.serialization.MoneyJson;
import com.github.namuan.fintech.time.FinancialTimestampSet;
import com.github.namuan.fintech.workflow.MoneyWorkflow;
import com.github.namuan.fintech.workflow.NamedWorkflowStep;
import com.github.namuan.fintech.workflow.WorkflowDriver;
import com.github.namuan.fintech.workflow.WorkflowState;
import com.github.namuan.fintech.workflow.WorkflowStore;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Group;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyBasedSafetyTest {
    private static final FiatCurrency USD = new FiatCurrency("USD", 2, "US Dollar");
    private static final FiatCurrency EUR = new FiatCurrency("EUR", 2, "Euro");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Group
    class MoneyProperties {
        @Property(tries = 200)
        @Label("same-asset money addition is associative and preserves total value")
        void additionIsAssociative(@ForAll("moneyValues") Money a, @ForAll("moneyValues") Money b, @ForAll("moneyValues") Money c) {
            Money left = a.plus(b).plus(c);
            Money right = a.plus(b.plus(c));

            assertSameMoneyValue(left, right);
        }

        @Property(tries = 200)
        @Label("subtracting a money value reverses adding it")
        void subtractionIsInverseOfAddition(@ForAll("moneyValues") Money a, @ForAll("moneyValues") Money b) {
            assertSameMoneyValue(a.plus(b).minus(b), a);
        }

        @Property(tries = 100)
        @Label("cross-asset arithmetic is always rejected")
        void crossAssetArithmeticRejected(@ForAll("nonNegativeMinorUnits") long units) {
            Money usd = Money.minorUnits(units, USD);
            Money eur = Money.minorUnits(units, EUR);

            assertThrows(IllegalArgumentException.class, () -> usd.plus(eur));
            assertThrows(IllegalArgumentException.class, () -> usd.minus(eur));
        }

        @Property(tries = 200)
        @Label("JSON-safe decimal representation round trips without numeric loss")
        void moneyJsonRoundTripPreservesValue(@ForAll("moneyValues") Money original) {
            MoneyJson json = MoneyJson.from(original);
            Money parsed = MoneyParser.decimalString(json.amount(), original.asset());

            assertEquals(original.asset(), parsed.asset());
            assertEquals(0, original.decimalValue().compareTo(parsed.decimalValue()));
            assertEquals(original.decimalValue().toPlainString(), json.amount());
            assertEquals(original.amount().scale(), json.scale());
        }
    }

    @Group
    class LedgerProperties {
        @Property(tries = 150)
        @Label("generated double-entry posting sequences keep derived balances in sync")
        void generatedPostingSequencesBalance(@ForAll @Size(min = 1, max = 40) List<@IntRange(min = 0, max = 1_000_000) Integer> cents) {
            Account cash = new Account(new AccountId("cash"), "Cash", AccountType.ASSET, USD);
            Account liability = new Account(new AccountId("liability"), "User Liability", AccountType.LIABILITY, USD);
            Ledger ledger = new Ledger(new InMemoryLedgerStore());

            BigDecimal expected = BigDecimal.ZERO;
            for (int amount : cents) {
                Money money = Money.minorUnits(amount, USD);
                ledger.post(journalEntry(
                        new Posting(cash.id(), DebitCredit.DEBIT, money, "cash"),
                        new Posting(liability.id(), DebitCredit.CREDIT, money, "liability")
                ));
                expected = expected.add(money.decimalValue());
            }

            assertEquals(0, expected.compareTo(ledger.balance(cash).decimalValue()));
            assertEquals(0, expected.compareTo(ledger.balance(liability).decimalValue()));
        }

        @Property(tries = 100)
        @Label("numeric equality balances postings even when BigDecimal scales differ")
        void equalValuesWithDifferentScalesStillBalance(@ForAll @IntRange(min = 0, max = 1_000_000) int cents) {
            AccountId debit = new AccountId("debit");
            AccountId credit = new AccountId("credit");
            String decimal = BigDecimal.valueOf(cents, 2).toPlainString();
            Money debitAmount = Money.decimal(decimal, USD);
            Money creditAmount = Money.decimal(decimal + "0", USD);

            JournalEntry entry = journalEntry(
                    new Posting(debit, DebitCredit.DEBIT, debitAmount, "debit"),
                    new Posting(credit, DebitCredit.CREDIT, creditAmount, "credit")
            );

            assertEquals(2, entry.postings().size());
        }

        @Property(tries = 100)
        @Label("reversing a generated entry nets involved balances to zero")
        void reversalNetsLedgerBalancesToZero(@ForAll("positiveMinorUnits") long cents) {
            Account cash = new Account(new AccountId("cash"), "Cash", AccountType.ASSET, USD);
            Account liability = new Account(new AccountId("liability"), "User Liability", AccountType.LIABILITY, USD);
            Ledger ledger = new Ledger(new InMemoryLedgerStore());
            JournalEntry original = journalEntry(
                    new Posting(cash.id(), DebitCredit.DEBIT, Money.minorUnits(cents, USD), "cash"),
                    new Posting(liability.id(), DebitCredit.CREDIT, Money.minorUnits(cents, USD), "liability")
            );

            ledger.post(original);
            ledger.post(ReversalEntry.reverse(original, FinancialTimestampSet.bookedNow(NOW), "reverse"));

            assertEquals(0, BigDecimal.ZERO.compareTo(ledger.balance(cash).decimalValue()));
            assertEquals(0, BigDecimal.ZERO.compareTo(ledger.balance(liability).decimalValue()));
        }

        @Property(tries = 100)
        @Label("unbalanced generated entries are rejected")
        void unbalancedEntriesRejected(@ForAll("positiveMinorUnits") long debitCents, @ForAll("positiveMinorUnits") long delta) {
            AccountId debit = new AccountId("debit");
            AccountId credit = new AccountId("credit");
            long creditCents = Math.addExact(debitCents, delta);

            assertThrows(IllegalArgumentException.class, () -> journalEntry(
                    new Posting(debit, DebitCredit.DEBIT, Money.minorUnits(debitCents, USD), "debit"),
                    new Posting(credit, DebitCredit.CREDIT, Money.minorUnits(creditCents, USD), "credit")
            ));
        }
    }

    @Group
    class ExecutionSafetyProperties {
        @Property(tries = 100)
        @Label("duplicate idempotent operations collapse into one side effect")
        void duplicateIdempotentOperationsCollapse(@ForAll @IntRange(min = 2, max = 25) int attempts, @ForAll("safeToken") String keyValue) {
            IdempotencyBarrier barrier = new IdempotencyBarrier(new InMemoryIdempotencyStore());
            IdempotencyScope scope = new IdempotencyScope("withdraw", "actor", "payments");
            IdempotencyKey key = new IdempotencyKey(keyValue);
            AtomicInteger sideEffects = new AtomicInteger();

            for (int i = 0; i < attempts; i++) {
                var result = barrier.execute(scope, key, "payload", () -> "effect-" + sideEffects.incrementAndGet());
                assertEquals("effect-1", result.value());
                assertEquals(i > 0, result.replayed());
            }

            assertEquals(1, sideEffects.get());
            assertThrows(IllegalArgumentException.class, () -> barrier.execute(scope, key, "different-payload", () -> "bad"));
        }

        @Property(tries = 60)
        @Label("workflow crashes at any persisted boundary can be resumed to completion")
        void workflowCrashBoundariesAreResumable(
                @ForAll @IntRange(min = 1, max = 8) int stepCount,
                @ForAll @IntRange(min = 1, max = 16) int crashOnSave
        ) throws Exception {
            CrashableWorkflowStore store = new CrashableWorkflowStore(crashOnSave);
            MoneyWorkflow workflow = workflowWithSteps(stepCount);

            try {
                new WorkflowDriver(store).run("wf", workflow);
            } catch (InjectedCrash ignored) {
                // Simulates process death after durable progress has been saved.
            }

            store.disableCrash();
            WorkflowState done = new WorkflowDriver(store).run("wf", workflow);

            assertTrue(done.completed());
            for (int step = 0; step < stepCount; step++) {
                assertEquals("true", done.data().get("step-" + step));
            }
        }
    }

    @Group
    class ReconciliationProperties {
        @Property(tries = 100)
        @Label("reconciliation reports no breaks for generated equal record sets")
        void equalRecordSetsReconcile(@ForAll @Size(min = 1, max = 30) List<@IntRange(min = 0, max = 1_000_000) Integer> cents) {
            List<ReconciliationRecord> left = records(cents, 0);
            List<ReconciliationRecord> right = records(cents, 0);

            assertTrue(new ReconciliationJob(MatchRule.sameIdAndAmount()).compare(() -> left, () -> right).isEmpty());
        }

        @Property(tries = 100)
        @Label("reconciliation catches generated missing and mismatched records")
        void missingAndMismatchedRecordsAreCaught(@ForAll @Size(min = 2, max = 30) List<@IntRange(min = 0, max = 1_000_000) Integer> cents) {
            List<ReconciliationRecord> left = records(cents, 0);
            List<ReconciliationRecord> right = new ArrayList<>(records(cents, 0));
            right.remove(right.size() - 1);
            right.set(0, new ReconciliationRecord("tx-0", "provider", Money.minorUnits(cents.get(0) + 1L, USD), NOW, Map.of()));

            List<ReconciliationBreak> breaks = new ReconciliationJob(MatchRule.sameIdAndAmount()).compare(() -> left, () -> right);

            assertFalse(breaks.isEmpty());
            assertTrue(breaks.stream().anyMatch(b -> b.description().contains("missing") || b.description().contains("different")));
        }
    }

    @Provide
    Arbitrary<Money> moneyValues() {
        return Arbitraries.longs().between(-1_000_000_000L, 1_000_000_000L)
                .flatMap(units -> Arbitraries.integers().between(0, 6)
                        .map(scale -> MoneyParser.mantissaAndScale(Long.toString(units), scale, USD)));
    }

    @Provide
    Arbitrary<Long> nonNegativeMinorUnits() {
        return Arbitraries.longs().between(0L, 1_000_000_000L);
    }

    @Provide
    Arbitrary<Long> positiveMinorUnits() {
        return Arbitraries.longs().between(1L, 1_000_000L);
    }

    @Provide
    Arbitrary<String> safeToken() {
        return Arbitraries.strings().withCharRange('a', 'z').ofMinLength(1).ofMaxLength(30);
    }

    private static JournalEntry journalEntry(Posting... postings) {
        return new JournalEntry(JournalEntryId.random(), List.of(postings), FinancialTimestampSet.bookedNow(NOW), "property", Map.of());
    }

    private static void assertSameMoneyValue(Money left, Money right) {
        assertEquals(left.asset(), right.asset());
        assertEquals(0, left.decimalValue().compareTo(right.decimalValue()));
    }

    private static MoneyWorkflow workflowWithSteps(int count) {
        List<NamedWorkflowStep> steps = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = i;
            steps.add(new NamedWorkflowStep("step-" + index, state -> {
                Map<String, String> data = new HashMap<>(state.data());
                data.put("step-" + index, "true");
                return new WorkflowState(state.workflowId(), "step-" + index, false, data);
            }));
        }
        return new MoneyWorkflow("generated", steps);
    }

    private static List<ReconciliationRecord> records(List<Integer> cents, int offset) {
        List<ReconciliationRecord> records = new ArrayList<>();
        for (int i = 0; i < cents.size(); i++) {
            records.add(new ReconciliationRecord("tx-" + (i + offset), "source", Money.minorUnits(cents.get(i), USD), NOW.plusSeconds(i), Map.of()));
        }
        return records;
    }

    private static final class CrashableWorkflowStore implements WorkflowStore {
        private final Map<String, WorkflowState> states = new HashMap<>();
        private final AtomicInteger saves = new AtomicInteger();
        private int crashOnSave;

        private CrashableWorkflowStore(int crashOnSave) {
            this.crashOnSave = crashOnSave;
        }

        @Override
        public void save(WorkflowState state) {
            states.put(state.workflowId(), state);
            if (crashOnSave == saves.incrementAndGet()) {
                throw new InjectedCrash();
            }
        }

        @Override
        public Optional<WorkflowState> load(String workflowId) {
            return Optional.ofNullable(states.get(workflowId));
        }

        private void disableCrash() {
            crashOnSave = -1;
        }
    }

    private static final class InjectedCrash extends RuntimeException {}
}
