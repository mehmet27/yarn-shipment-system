package com.yarnshipment.audit.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.audit.TestTokens;
import com.yarnshipment.audit.application.port.in.AuditRecordPage;
import com.yarnshipment.audit.application.port.in.ListAuditRecordsQuery;
import com.yarnshipment.audit.application.port.in.ListAuditRecordsUseCase;
import com.yarnshipment.audit.application.port.in.ListChainChecksUseCase;
import com.yarnshipment.audit.domain.model.AuditRecord;
import com.yarnshipment.audit.domain.model.ChainCheck;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Denetim okuma uçlarının sözleşmesi ve izin denetimi (AUDIT_VIEW). */
@WebMvcTest(controllers = AuditLogController.class)
class AuditLogControllerTest {

    private static final String AUDIT_LOG = "/api/v1/audit-log";
    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-0000000000b1");
    private static final UUID ACTOR = UUID.fromString("0192f0a0-0000-7000-8000-00000000d001");
    private static final long RECORD_ID = 7L;
    private static final int PAGE_SIZE = 20;
    private static final int REQUESTED_PAGE = 2;
    private static final int REQUESTED_SIZE = 5;
    private static final long BROKEN_ID = 3L;
    private static final Instant AT = Instant.parse("2026-11-03T08:00:00Z");

    private final MockMvcTester mvc;

    @MockitoBean
    private ListAuditRecordsUseCase listAuditRecords;

    @MockitoBean
    private ListChainChecksUseCase listChainChecks;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    AuditLogControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Kayıt listesi aktör, varlık, gövde ve sayfa bilgisiyle döner")
    void shouldListRecords() {
        AuditRecord record = new AuditRecord(
                RECORD_ID,
                AT,
                AT.plusSeconds(1),
                new AuditRecord.Origin("shipment-service", UUID.randomUUID(), "corr-1"),
                new AuditRecord.Actor(ACTOR, "WAREHOUSE", null),
                "com.yarnshipment.shipment.shipment-loaded.v1",
                null,
                new AuditRecord.Entity("Shipment", SHIPMENT),
                null,
                Map.of("status", "LOADED"));
        when(listAuditRecords.listAuditRecords(any()))
                .thenReturn(new AuditRecordPage(List.of(record), 0, PAGE_SIZE, 1));

        MvcTestResult result =
                mvc.get().uri(AUDIT_LOG).with(TestTokens.auth("manager")).exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.items[0].id").isEqualTo((int) RECORD_ID);
        assertThat(result).bodyJson().extractingPath("$.items[0].occurredAt").isEqualTo("2026-11-03T08:00:00Z");
        assertThat(result).bodyJson().extractingPath("$.items[0].actor.userId").isEqualTo(ACTOR.toString());
        assertThat(result).bodyJson().extractingPath("$.items[0].entity.type").isEqualTo("Shipment");
        assertThat(result).bodyJson().extractingPath("$.items[0].after.status").isEqualTo("LOADED");
        assertThat(result).bodyJson().extractingPath("$.page.totalElements").isEqualTo(1);
    }

    @Test
    @DisplayName("Süzgeçler ve sayfa bilgisi use case'e taşınır")
    void shouldPassFilters() {
        when(listAuditRecords.listAuditRecords(any()))
                .thenReturn(new AuditRecordPage(List.of(), REQUESTED_PAGE, REQUESTED_SIZE, 0));

        MvcTestResult result = mvc.get()
                .uri(AUDIT_LOG
                        + "?entityType=Shipment&entityId=" + SHIPMENT + "&actorUserId=" + ACTOR
                        + "&action=x&sourceService=shipment-service&from=2026-11-01T00:00:00Z"
                        + "&to=2026-11-04T00:00:00Z&page=2&size=5")
                .with(TestTokens.auth("admin"))
                .exchange();

        assertThat(result).hasStatusOk();
        ArgumentCaptor<ListAuditRecordsQuery> query = ArgumentCaptor.forClass(ListAuditRecordsQuery.class);
        verify(listAuditRecords).listAuditRecords(query.capture());
        ListAuditRecordsQuery.Filter filter = query.getValue().filter();
        assertThat(filter.entityType()).isEqualTo("Shipment");
        assertThat(filter.entityId()).isEqualTo(SHIPMENT);
        assertThat(filter.actorUserId()).isEqualTo(ACTOR);
        assertThat(filter.action()).isEqualTo("x");
        assertThat(filter.sourceService()).isEqualTo("shipment-service");
        assertThat(filter.from()).isEqualTo(Instant.parse("2026-11-01T00:00:00Z"));
        assertThat(filter.to()).isEqualTo(Instant.parse("2026-11-04T00:00:00Z"));
        assertThat(query.getValue().page()).isEqualTo(REQUESTED_PAGE);
        assertThat(query.getValue().size()).isEqualTo(REQUESTED_SIZE);
    }

    @Test
    @DisplayName("Zincir doğrulama sonuçları bozulma bilgisiyle döner")
    void shouldListChainChecks() {
        when(listChainChecks.listChainChecks(anyInt()))
                .thenReturn(List.of(
                        new ChainCheck(2, AT, 1, REQUESTED_SIZE, false, BROKEN_ID),
                        new ChainCheck(1, AT, 1, REQUESTED_SIZE, true, null)));

        MvcTestResult result = mvc.get()
                .uri(AUDIT_LOG + "/chain-checks")
                .with(TestTokens.auth("admin"))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[0].ok").isEqualTo(false);
        assertThat(result).bodyJson().extractingPath("$[0].brokenAtId").isEqualTo((int) BROKEN_ID);
        assertThat(result).bodyJson().extractingPath("$[1].ok").isEqualTo(true);
    }
}
