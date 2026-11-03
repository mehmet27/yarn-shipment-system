package com.yarnshipment.discrepancy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.discrepancy.TestClaims;
import com.yarnshipment.discrepancy.TestDiscrepancies;
import com.yarnshipment.discrepancy.TestTokens;
import com.yarnshipment.discrepancy.application.port.in.claim.GetShipmentClaimsQuery;
import com.yarnshipment.discrepancy.application.port.in.claim.RecordInspectionResultCommand;
import com.yarnshipment.discrepancy.application.port.in.claim.RequestInspectionCommand;
import com.yarnshipment.discrepancy.application.port.in.claim.ShipmentClaims;
import com.yarnshipment.discrepancy.application.port.out.InspectionPort.NewInspection;
import com.yarnshipment.discrepancy.domain.claim.ClaimRejection;
import com.yarnshipment.discrepancy.domain.claim.Inspection;
import com.yarnshipment.discrepancy.domain.claim.InspectionKind;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyNotOpenException;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InspectionServicesTest {

    private final ClaimPorts ports = new ClaimPorts();
    private final GetShipmentClaimsService get = new GetShipmentClaimsService(
            ports.claims, ports.discrepancies, ports.inspections, ports.scopeGuard, ClaimPorts.CLOCK);
    private final RequestInspectionService request = new RequestInspectionService(
            ports.claims, ports.discrepancies, ports.inspections, ports.scopeGuard, ClaimPorts.CLOCK);
    private final RecordInspectionResultService record = new RecordInspectionResultService(
            ports.claims, ports.discrepancies, ports.inspections, ports.evidence, ports.scopeGuard, ClaimPorts.CLOCK);

    @Test
    @DisplayName("İtiraz dosyası süreyi, itirazları ve kontrollerini döner")
    void shouldReadShipmentClaims() {
        when(ports.claims.findClaims(TestDiscrepancies.SHIPMENT))
                .thenReturn(List.of(TestClaims.claim(List.of("UK-0001"))));
        when(ports.inspections.findInspections(TestDiscrepancies.ID))
                .thenReturn(List.of(TestClaims.pending(InspectionKind.SAMPLE)));

        ShipmentClaims file = get.getShipmentClaims(new GetShipmentClaimsQuery(
                TestTokens.actor(TestTokens.subcontractorToken()), TestDiscrepancies.SHIPMENT));

        assertThat(file.windowOpen()).isTrue();
        assertThat(file.deadline()).isEqualTo(TestClaims.target().deadline());
        assertThat(file.claims().getFirst().inspections()).hasSize(1);
    }

    @Test
    @DisplayName("Kalite açık itiraza kontrol ister; karara bağlanmış itiraza isteyemez")
    void shouldRequestInspection() {
        when(ports.inspections.saveRequest(any())).thenReturn(TestClaims.pending(InspectionKind.LAB));

        request.requestInspection(
                new RequestInspectionCommand(quality(), TestClaims.CLAIM_ID, InspectionKind.LAB, "  lab'a gönder "));

        ArgumentCaptor<NewInspection> saved = ArgumentCaptor.forClass(NewInspection.class);
        verify(ports.inspections).saveRequest(saved.capture());
        assertThat(saved.getValue().request().note()).isEqualTo("lab'a gönder");
        assertThat(saved.getValue().discrepancyId()).isEqualTo(TestDiscrepancies.ID);

        when(ports.discrepancies.findDiscrepancy(TestDiscrepancies.ID))
                .thenReturn(Optional.of(TestDiscrepancies.withStatus(TestTokens.SITE_ID, DiscrepancyStatus.RESOLVED)));
        assertThatThrownBy(() -> request.requestInspection(
                        new RequestInspectionCommand(quality(), TestClaims.CLAIM_ID, InspectionKind.SAMPLE, null)))
                .isInstanceOf(DiscrepancyNotOpenException.class);
    }

    @Test
    @DisplayName("Lab sonucu aynı sevkiyatın LAB_RESULT belgesiyle girilir")
    void shouldRecordLabResult() {
        when(ports.inspections.findInspection(TestClaims.INSPECTION_ID))
                .thenReturn(Optional.of(TestClaims.pending(InspectionKind.LAB)));

        assertThat(TestClaims.rejectionOf(() -> record.recordInspectionResult(result(TestClaims.PHOTO))))
                .isEqualTo(ClaimRejection.EVIDENCE_INVALID);
        assertThat(TestClaims.rejectionOf(() -> record.recordInspectionResult(result(null))))
                .isEqualTo(ClaimRejection.RESULT_EVIDENCE_REQUIRED);

        Inspection recorded = record.recordInspectionResult(result(TestClaims.LAB_DOCUMENT));

        assertThat(recorded.isPending()).isFalse();
        verify(ports.inspections).saveResult(recorded);
    }

    private static RecordInspectionResultCommand result(@Nullable UUID evidenceId) {
        return new RecordInspectionResultCommand(
                quality(), TestClaims.CLAIM_ID, TestClaims.INSPECTION_ID, " B kalite doğrulandı ", evidenceId);
    }

    private static Actor quality() {
        return TestTokens.actor("quality");
    }
}
