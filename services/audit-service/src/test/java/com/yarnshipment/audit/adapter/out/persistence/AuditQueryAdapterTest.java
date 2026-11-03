package com.yarnshipment.audit.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.audit.application.port.in.AuditRecordPage;
import com.yarnshipment.audit.application.port.in.ListAuditRecordsQuery;
import com.yarnshipment.audit.application.port.in.ListAuditRecordsQuery.Filter;
import com.yarnshipment.audit.domain.model.AuditRecord;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Okuma rolüyle süzme, sıralama ve sayfalama; kayıtlar yazan rolle commit edilir. */
@AuditAdapterTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditQueryAdapterTest {

    private static final PostgresTestDatabase DATABASE = PostgresTestDatabase.start();
    private static final Instant FIRST_AT = Instant.parse("2026-11-03T08:00:00Z");
    private static final Instant SECOND_AT = FIRST_AT.plusSeconds(60);
    private static final Instant THIRD_AT = FIRST_AT.plusSeconds(120);
    private static final int PAGE_SIZE = 10;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private AuditAppendAdapter appender;

    @Autowired
    private AuditQueryAdapter queries;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        DATABASE.registerIn(registry);
    }

    /** İki kullanıcı kaydı ve bir servis hesabı kaydı; sıra numaraları artan sırada atanır. */
    @BeforeAll
    void seed() {
        appender.append(AuditEntries.byUser(UUID.randomUUID(), FIRST_AT, AuditEntries.LOADED, AuditEntries.SHIPMENT));
        appender.append(AuditEntries.bySystem(SECOND_AT, AuditEntries.DELIVERED, AuditEntries.OTHER_SHIPMENT));
        appender.append(AuditEntries.byUser(UUID.randomUUID(), THIRD_AT, AuditEntries.LOADED, AuditEntries.SHIPMENT));
    }

    @Test
    @DisplayName("Okuma aktörü, varlığı, olay gövdesini ve kullanıcısız servis hesabını geri verir")
    void shouldReadWhatWasAppended() {
        AuditRecordPage page =
                queries.findRecords(query(new Filter(null, AuditEntries.OTHER_SHIPMENT, null, null, null, null, null)));

        assertThat(page.totalElements()).isEqualTo(1);
        AuditRecord record = page.items().getFirst();
        assertThat(record.actor().userId()).isNull();
        assertThat(record.actor().clientId()).isEqualTo(AuditEntries.SYSTEM_CLIENT);
        assertThat(record.action()).isEqualTo(AuditEntries.DELIVERED);
        assertThat(record.entity()).isEqualTo(new AuditRecord.Entity("Shipment", AuditEntries.OTHER_SHIPMENT));
        assertThat(record.after()).containsEntry("status", "AuditEntries.LOADED");
        assertThat(record.origin().traceId()).isEqualTo("corr-1");
        assertThat(record.occurredAt()).isEqualTo(SECOND_AT);
        assertThat(record.recordedAt()).isAfterOrEqualTo(record.occurredAt().minusSeconds(1));
    }

    @Test
    @DisplayName("Kayıtlar en yeni önce ve sayfa sayfa gelir")
    void shouldPageNewestFirst() {
        Filter byActor = new Filter(null, null, AuditEntries.ACTOR, null, null, null, null);

        AuditRecordPage first = queries.findRecords(new ListAuditRecordsQuery(byActor, 0, 1));
        AuditRecordPage second = queries.findRecords(new ListAuditRecordsQuery(byActor, 1, 1));

        assertThat(first.totalElements()).isEqualTo(2);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(first.items().getFirst().occurredAt()).isEqualTo(THIRD_AT);
        assertThat(second.items().getFirst().occurredAt()).isEqualTo(FIRST_AT);
    }

    @Test
    @DisplayName("Zaman aralığı dahil/hariç uygulanır, süzgeçler birlikte daraltır")
    void shouldCombineFilters() {
        Filter untilSecond = new Filter(null, null, null, null, null, null, SECOND_AT);
        Filter fromSecond = new Filter(null, null, null, null, null, SECOND_AT, null);
        Filter wrongService = new Filter(null, null, null, AuditEntries.DELIVERED, "shipment-service", null, null);
        Filter rightService =
                new Filter("Shipment", null, null, AuditEntries.DELIVERED, AuditEntries.SYSTEM_CLIENT, null, null);

        assertThat(queries.findRecords(query(untilSecond)).totalElements()).isEqualTo(1);
        assertThat(queries.findRecords(query(fromSecond)).totalElements()).isEqualTo(2);
        assertThat(queries.findRecords(query(wrongService)).totalElements()).isZero();
        assertThat(queries.findRecords(query(rightService)).totalElements()).isEqualTo(1);
    }

    private static ListAuditRecordsQuery query(Filter filter) {
        return new ListAuditRecordsQuery(filter, 0, PAGE_SIZE);
    }
}
