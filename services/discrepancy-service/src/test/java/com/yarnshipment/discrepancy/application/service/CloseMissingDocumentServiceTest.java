package com.yarnshipment.discrepancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.discrepancy.TestDiscrepancies;
import com.yarnshipment.discrepancy.application.port.in.CloseMissingDocumentCommand;
import com.yarnshipment.discrepancy.application.port.in.DiscrepancyPage;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyDecisionPort;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyStatus;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyType;
import com.yarnshipment.discrepancy.domain.model.Resolution;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CloseMissingDocumentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-24T10:00:00Z");
    private static final int PAGE_SIZE = 100;
    private static final UUID EVIDENCE = UUID.fromString("0192f0a0-0000-7000-8000-00000000f003");
    private static final UUID MISSING = UUID.fromString("0192f0a0-0000-7000-8000-0000000d0011");
    private static final UUID MISMATCH = UUID.fromString("0192f0a0-0000-7000-8000-0000000d0012");
    private static final UUID RESOLVED = UUID.fromString("0192f0a0-0000-7000-8000-0000000d0013");

    private final DiscrepancyQueryPort queries = mock(DiscrepancyQueryPort.class);
    private final DiscrepancyDecisionPort decisions = mock(DiscrepancyDecisionPort.class);
    private final CloseMissingDocumentService service =
            new CloseMissingDocumentService(queries, decisions, Clock.fixed(NOW, ZoneOffset.UTC));
    private final CloseMissingDocumentCommand command =
            new CloseMissingDocumentCommand(TestDiscrepancies.SHIPMENT, EVIDENCE);

    @Test
    @DisplayName(
            "Açık DOCUMENT_MISSING sistem hesabıyla veri hatası kararıyla kapanır; diğer belge kuralları açık kalır")
    void shouldCloseOnlyOpenMissingDocument() {
        givenShipmentDiscrepancies(
                TestDiscrepancies.withRule(
                        MISSING, DiscrepancyType.DOCUMENT, "DOCUMENT_MISSING", DiscrepancyStatus.IN_REVIEW),
                TestDiscrepancies.withRule(
                        MISMATCH, DiscrepancyType.DOCUMENT, "DOCUMENT_NET_KG_MISMATCH", DiscrepancyStatus.OPEN));

        int closed = service.closeMissingDocument(command);

        ArgumentCaptor<Discrepancy> saved = ArgumentCaptor.forClass(Discrepancy.class);
        verify(decisions).saveResolution(saved.capture(), eq(CloseMissingDocumentService.SYSTEM_ACTOR), eq(NOW));
        Discrepancy.Decision decision = Objects.requireNonNull(saved.getValue().decision());
        assertThat(closed).isEqualTo(1);
        assertThat(saved.getValue().id()).isEqualTo(MISSING);
        assertThat(saved.getValue().status()).isEqualTo(DiscrepancyStatus.RESOLVED);
        assertThat(decision.resolution()).isEqualTo(Resolution.DATA_ERROR);
        assertThat(decision.note()).contains(EVIDENCE.toString());
        assertThat(decision.resolvedBy()).isEqualTo(new UUID(0L, 0L));
    }

    @Test
    @DisplayName("Zaten kapanmış uyuşmazlık tekrar yazılmaz; olay tekrar gelse de hiçbir şey değişmez")
    void shouldIgnoreResolvedDiscrepancies() {
        givenShipmentDiscrepancies(TestDiscrepancies.withRule(
                RESOLVED, DiscrepancyType.DOCUMENT, "DOCUMENT_MISSING", DiscrepancyStatus.RESOLVED));

        assertThat(service.closeMissingDocument(command)).isZero();
        verify(decisions, never()).saveResolution(any(), any(), any());
    }

    private void givenShipmentDiscrepancies(Discrepancy... discrepancies) {
        when(queries.findDiscrepancies(any(), eq(AccessScope.UNRESTRICTED), eq(0), eq(PAGE_SIZE)))
                .thenReturn(new DiscrepancyPage(List.of(discrepancies), 0, PAGE_SIZE, discrepancies.length));
    }
}
