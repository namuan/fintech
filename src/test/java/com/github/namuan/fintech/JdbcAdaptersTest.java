package com.github.namuan.fintech;

import com.github.namuan.fintech.audit.AuditTrailStore;
import com.github.namuan.fintech.idempotency.IdempotencyStore;
import com.github.namuan.fintech.ledger.LedgerStore;
import com.github.namuan.fintech.outbox.ConsumerDeduplicationStore;
import com.github.namuan.fintech.outbox.OutboxStore;
import com.github.namuan.fintech.reservations.ReservationStore;
import com.github.namuan.fintech.storage.jdbc.*;
import com.github.namuan.fintech.webhooks.RawWebhookStore;
import com.github.namuan.fintech.workflow.WorkflowStore;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JdbcAdaptersTest {
    private final DataSource dataSource = new ThrowingDataSource();

    @Test
    void jdbcAdaptersImplementStoreContracts() {
        assertInstanceOf(LedgerStore.class, new JdbcLedgerStore(dataSource));
        assertInstanceOf(IdempotencyStore.class, new JdbcIdempotencyStore(dataSource));
        assertInstanceOf(ReservationStore.class, new JdbcReservationStore(dataSource));
        assertInstanceOf(WorkflowStore.class, new JdbcWorkflowStore(dataSource));
        assertInstanceOf(RawWebhookStore.class, new JdbcRawWebhookStore(dataSource));
        assertInstanceOf(OutboxStore.class, new JdbcOutboxStore(dataSource));
        assertInstanceOf(ConsumerDeduplicationStore.class, new JdbcConsumerDeduplicationStore(dataSource));
        assertInstanceOf(AuditTrailStore.class, new JdbcAuditTrailStore(dataSource));
    }

    @Test
    void postgresqlSchemaIsPackaged() {
        assertNotNull(Thread.currentThread().getContextClassLoader().getResource("db/postgresql/schema.sql"));
    }
}
