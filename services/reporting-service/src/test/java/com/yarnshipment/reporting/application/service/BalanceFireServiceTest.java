package com.yarnshipment.reporting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.audit.AuditPublisher;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.reporting.TestTokens;
import com.yarnshipment.reporting.application.port.in.ReportQuery;
import com.yarnshipment.reporting.application.port.out.BalanceFirePort;
import com.yarnshipment.reporting.domain.model.FireFile;
import com.yarnshipment.reporting.domain.model.OpeningBalanceRow;
import com.yarnshipment.reporting.domain.model.OpeningImportResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Bakiye matrisi, fire raporları ve açılış bakiyesi içe aktarma servisi. */
class BalanceFireServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"), ZoneOffset.UTC);

    private final BalanceFirePort port = mock(BalanceFirePort.class);
    private final BalanceFireService service =
            new BalanceFireService(port, new ReportAccess(new ScopeGuard(mock(AuditPublisher.class), CLOCK)), CLOCK);

    @Test
    @DisplayName("Fasoncu rapor görmez, sorgu çalışmaz")
    void shouldReturnEmptyForExternalRoles() {
        ReportQuery query = query(TestTokens.actor(TestTokens.subcontractorToken()));

        assertThat(service.listBalanceMatrix(query)).isEmpty();
        assertThat(service.listFire(query, null)).isEmpty();
        assertThat(service.listYarnVsFabric(query)).isEmpty();
        assertThat(service.listToInvoice(query)).isEmpty();
        verifyNoInteractions(port);
    }

    @Test
    @DisplayName("Faturalanacaklar yalnız eksiği olan TO_INVOICE dosyalarıdır")
    void shouldKeepOnlyShortageFilesToInvoice() {
        UUID sub = UUID.randomUUID();
        FireFile short1 = file(sub, "900", "TO_INVOICE");
        FireFile ok = file(sub, "960", "TO_INVOICE");
        FireFile closed = file(sub, "900", "CLOSED");
        when(port.findFireFiles(any(), any(), eq(null))).thenReturn(List.of(short1, ok, closed));

        assertThat(service.listToInvoice(query(TestTokens.actor("manager")))).containsExactly(short1);
        assertThat(service.listYarnVsFabric(query(TestTokens.actor("manager")))).hasSize(1);
    }

    @Test
    @DisplayName("Açılış bakiyesi: var olan (fasoncu, iplik) atlanır")
    void shouldCountImportedAndSkipped() {
        OpeningBalanceRow written = new OpeningBalanceRow(UUID.randomUUID(), "A", "Viskon", new BigDecimal("10.000"));
        OpeningBalanceRow existing = new OpeningBalanceRow(UUID.randomUUID(), "B", "Poly", new BigDecimal("5.000"));
        when(port.insertOpeningIfAbsent(eq(written), any())).thenReturn(true);
        when(port.insertOpeningIfAbsent(eq(existing), any())).thenReturn(false);

        OpeningImportResult result = service.importOpening(LocalDate.parse("2026-11-09"), List.of(written, existing));

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
    }

    private static FireFile file(UUID sub, String fabric, String status) {
        return new FireFile(
                UUID.randomUUID(),
                "SHP",
                sub,
                "K",
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                new BigDecimal(fabric),
                new BigDecimal("0.04"),
                status,
                null,
                null);
    }

    private static ReportQuery query(com.yarnshipment.common.security.Actor actor) {
        return new ReportQuery(actor, null, null, null);
    }
}
