package com.yarnshipment.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Gerçek PostgreSQL'de kilitli okuma, işaretleme ve idempotent tüketici kaydı. */
class JdbcOutboxStoreTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private static final String INSERT = """
            INSERT INTO outbox_event (id, created_at, aggregate_type, aggregate_id, event_type, payload, headers,
                                      occurred_at, published_at)
            VALUES (:id, :createdAt, 'Shipment', :aggregateId, :type, '{"bagCount":40}'::jsonb,
                    '{"actorId":"user-1"}'::jsonb, :createdAt, :publishedAt)""";

    private static final DataSource DATA_SOURCE = PostgresTestDatabase.dataSource();
    private static final int LIMIT = 10;

    private final JdbcClient jdbc = JdbcClient.create(DATA_SOURCE);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final JdbcOutboxStore store = new JdbcOutboxStore(jdbc, JsonMapper.shared(), clock);
    private final TransactionTemplate transactions =
            new TransactionTemplate(new DataSourceTransactionManager(DATA_SOURCE));

    @BeforeEach
    void cleanTables() {
        jdbc.sql("TRUNCATE outbox_event, processed_event").update();
    }

    @Test
    @DisplayName("Yalnızca gönderilmemiş olaylar oluşturma sırasıyla okunur ve başlıklar çözülür")
    void shouldReadUnpublishedInOrder() {
        // given
        UUID older = insert(Instant.parse("2026-10-21T09:00:00Z"), null);
        UUID newer = insert(Instant.parse("2026-10-21T09:05:00Z"), null);
        insert(Instant.parse("2026-10-21T08:00:00Z"), NOW);

        // when
        List<OutboxEvent> events = transactions.execute(status -> store.lockUnpublished(LIMIT));

        // then
        assertThat(events).extracting(OutboxEvent::id).containsExactly(older, newer);
        assertThat(events.getFirst().headers()).containsEntry("actorId", "user-1");
        assertThat(events.getFirst().payload()).contains("\"bagCount\"");
    }

    @Test
    @DisplayName("İşaretlenen olay gönderildi zamanını alır ve bir daha okunmaz")
    void shouldMarkPublished() {
        // given
        insert(Instant.parse("2026-10-21T09:00:00Z"), null);

        // when
        transactions.executeWithoutResult(status -> store.markPublished(store.lockUnpublished(LIMIT)));

        // then
        assertThat(jdbc.sql("SELECT published_at FROM outbox_event")
                        .query(Timestamp.class)
                        .single())
                .isEqualTo(Timestamp.from(NOW));
        List<OutboxEvent> remaining = transactions.execute(status -> store.lockUnpublished(LIMIT));
        assertThat(remaining).isEmpty();
    }

    @Test
    @DisplayName("Başka transaction'ın kilitlediği olay atlanır")
    void shouldSkipLockedRows() {
        // given
        insert(Instant.parse("2026-10-21T09:00:00Z"), null);

        // when
        TransactionTemplate other = new TransactionTemplate(new DataSourceTransactionManager(DATA_SOURCE));
        other.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        List<List<OutboxEvent>> seen = transactions.execute(
                outer -> List.of(store.lockUnpublished(LIMIT), other.execute(inner -> store.lockUnpublished(LIMIT))));

        // then
        assertThat(seen.get(0)).hasSize(1);
        assertThat(seen.get(1)).isEmpty();
    }

    @Test
    @DisplayName("İdempotent tüketici aynı olayı yalnızca bir kez işlenmiş sayar")
    void shouldRecordProcessedEventOnce() {
        // given
        ProcessedEvents processed = new ProcessedEvents(jdbc, clock);
        UUID eventId = UUID.randomUUID();

        // when
        boolean first = processed.markProcessed(eventId, TestEvents.TYPE);
        boolean second = processed.markProcessed(eventId, TestEvents.TYPE);

        // then
        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }

    private UUID insert(Instant createdAt, @Nullable Instant publishedAt) {
        UUID id = UUID.randomUUID();
        jdbc.sql(INSERT)
                .param("id", id)
                .param("createdAt", Timestamp.from(createdAt))
                .param("aggregateId", TestEvents.SHIPMENT_ID)
                .param("type", TestEvents.TYPE)
                .param("publishedAt", publishedAt == null ? null : Timestamp.from(publishedAt))
                .update();
        return id;
    }
}
