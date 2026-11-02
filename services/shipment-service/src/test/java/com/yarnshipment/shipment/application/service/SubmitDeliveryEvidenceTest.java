package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestDeliveries;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.SubmitDeliveryReceiptCommand;
import com.yarnshipment.shipment.domain.evidence.EvidenceReference;
import com.yarnshipment.shipment.domain.evidence.InvalidDeliveryEvidenceException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Teslim girişine eklenen imzalı irsaliye kanıtının doğrulanması. */
class SubmitDeliveryEvidenceTest {

    private static final UUID KEY = UUID.fromString("0192f0a0-0000-7000-8000-0000000000dd");
    private static final UUID EVIDENCE = UUID.fromString("0192f0a0-0000-7000-8000-0000000e0001");

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-22T09:00:00Z"), ZoneOffset.UTC);
    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final RecordingDeliveryCommandPort commandPort = new RecordingDeliveryCommandPort();
    private final Actor subcontractor = TestTokens.CONVERTER.toActor(TestTokens.subcontractorToken());

    @Test
    @DisplayName("Bu sevkiyatın imzalı irsaliyesi olan kanıt kabul edilir")
    void shouldAcceptSignedDocumentOfShipment() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        EvidenceReference found = reference(TestShipments.SHIPMENT_ID, EvidenceReference.SIGNED_DELIVERY_DOC, "VALID");

        service(Optional.of(found)).submitDeliveryReceipt(command());

        assertThat(commandPort.receipts()).hasSize(1);
    }

    @Test
    @DisplayName(
            "Kanıt yoksa, başka sevkiyata aitse, imzalı irsaliye değilse ya da geçersiz kılınmışsa teslim kaydedilmez")
    void shouldRejectInvalidEvidence() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        EvidenceReference otherShipment = reference(UUID.randomUUID(), EvidenceReference.SIGNED_DELIVERY_DOC, "VALID");
        EvidenceReference otherType = reference(TestShipments.SHIPMENT_ID, "SEAL", "VALID");
        EvidenceReference invalidated =
                reference(TestShipments.SHIPMENT_ID, EvidenceReference.SIGNED_DELIVERY_DOC, "INVALIDATED");

        for (Optional<EvidenceReference> answer : java.util.List.of(
                Optional.<EvidenceReference>empty(),
                Optional.of(otherShipment),
                Optional.of(otherType),
                Optional.of(invalidated))) {
            assertThatThrownBy(() -> service(answer).submitDeliveryReceipt(command()))
                    .isInstanceOf(InvalidDeliveryEvidenceException.class);
        }
        assertThat(commandPort.receipts()).isEmpty();
    }

    private static EvidenceReference reference(UUID shipmentId, String type, String status) {
        return new EvidenceReference(EVIDENCE, shipmentId, type, status);
    }

    private SubmitDeliveryReceiptService service(Optional<EvidenceReference> answer) {
        return new SubmitDeliveryReceiptService(
                new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
                commandPort,
                id -> answer,
                clock);
    }

    private SubmitDeliveryReceiptCommand command() {
        return new SubmitDeliveryReceiptCommand(
                subcontractor, TestShipments.SHIPMENT_ID, KEY, TestDeliveries.measurementWithSignedDocument(EVIDENCE));
    }
}
