package com.github.namuan.fintech;

import com.github.namuan.fintech.controls.*;
import com.github.namuan.fintech.currency.FiatCurrency;
import com.github.namuan.fintech.fx.*;
import com.github.namuan.fintech.money.Money;
import com.github.namuan.fintech.outbox.*;
import com.github.namuan.fintech.reconciliation.*;
import com.github.namuan.fintech.rounding.RoundingPolicy;
import com.github.namuan.fintech.webhooks.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RemainingPackagesTest {
    private final FiatCurrency usd = new FiatCurrency("USD", 2, "US Dollar");
    private final FiatCurrency eur = new FiatCurrency("EUR", 2, "Euro");

    @Test
    void convertsFxUsingDirectionalQuoteAndRounding() {
        FxRate rate = new FxRate(new RateDirection(eur, usd), new BigDecimal("1.1234"), Instant.now(), new RateSource("test", "mid"));
        FxQuote quote = new FxQuote("q1", rate, Instant.now().plusSeconds(60));

        TransactionalConversion conversion = TransactionalConversion.convert(Money.decimal("10.00", eur), quote, new RoundingPolicy("usd-cents", 2, RoundingMode.HALF_UP));

        assertEquals("11.23", conversion.target().decimalValue().toPlainString());
        assertEquals(usd, conversion.target().asset());
    }

    @Test
    void persistsRawWebhookBeforeProcessing() {
        InMemoryRawWebhookStore store = new InMemoryRawWebhookStore();
        WebhookIngestionService service = new WebhookIngestionService(store, WebhookSignatureVerifier.trustForTests());
        WebhookEnvelope envelope = new WebhookEnvelope("evt-1", "psp", Instant.now(), Map.of("sig", "ok"), "{}".getBytes());

        service.ingest(envelope);

        assertTrue(store.find("evt-1").isPresent());
    }

    @Test
    void outboxRelayPublishesPendingEventsAtLeastOnce() throws Exception {
        InMemoryOutboxStore store = new InMemoryOutboxStore();
        store.append(new OutboxEvent(PublishedEventId.random(), "payments", "{}", Instant.now(), Map.of()));
        AtomicInteger published = new AtomicInteger();

        new OutboxRelay(store, event -> published.incrementAndGet()).drain();

        assertEquals(1, published.get());
        assertTrue(store.pending().isEmpty());
    }

    @Test
    void reconciliationFindsMissingRecord() {
        ReconciliationRecord left = new ReconciliationRecord("tx1", "ledger", Money.decimal("1.00", usd), Instant.now(), Map.of());
        ReconciliationRecord right = new ReconciliationRecord("tx2", "provider", Money.decimal("1.00", usd), Instant.now(), Map.of());

        List<ReconciliationBreak> breaks = new ReconciliationJob(MatchRule.sameIdAndAmount()).compare(() -> List.of(left), () -> List.of(right));

        assertEquals(2, breaks.size());
    }

    @Test
    void makerCheckerPreventsRequesterSelfApproval() {
        MakerCheckerRequest request = new MakerCheckerRequest("r1", "alice", "large-withdrawal", Instant.now());
        ApprovalPolicy policy = new ApprovalPolicy(1, false);

        assertThrows(IllegalArgumentException.class, () -> request.approve("alice", policy));
        request.approve("bob", policy);
        assertTrue(request.approved(policy));
    }
}
