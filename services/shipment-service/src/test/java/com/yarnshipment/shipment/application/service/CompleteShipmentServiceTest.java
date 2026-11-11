package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.CompleteShipmentCommand;
import com.yarnshipment.shipment.domain.completion.Completion;
import com.yarnshipment.shipment.domain.evidence.EvidenceReference;
import com.yarnshipment.shipment.domain.evidence.InvalidWaybillEvidenceException;
import com.yarnshipment.shipment.domain.fire.FireSettlementStatus;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
class CompleteShipmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-10T09:00:00Z");
    private static final UUID EVIDENCE = UUID.fromString("0192f0a0-0000-7000-8000-0000000000ee");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final List<Completion> completions = new ArrayList<>();
    private final Map<UUID, EvidenceReference> evidence = new HashMap<>();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final Actor manager = TestTokens.actor("manager");
    private final RecordingLedgerPorts ledger = new RecordingLedgerPorts();
    private final InMemoryFireSettlementPort fireSettlements = new InMemoryFireSettlementPort();
    private final CompleteShipmentService service = new CompleteShipmentService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
            (completion, actorUserId) -> completions.add(completion),
            id -> Optional.ofNullable(evidence.get(id)),
            ledger,
            fireSettlements,
            clock);

    private static CompleteShipmentCommand command(Actor actor, String waybillNo) {
        return new CompleteShipmentCommand(actor, TestShipments.SHIPMENT_ID, waybillNo, EVIDENCE, UUID.randomUUID());
    }

    private void givenEvidence(UUID shipmentId, String type) {
        evidence.put(EVIDENCE, new EvidenceReference(EVIDENCE, shipmentId, type, EvidenceReference.VALID));
    }

    @Test
    @DisplayName("Müdür teslim bekleyen sevkiyatı irsaliye belgesiyle sunucu saatinde tamamlar")
    void shouldCompleteWithWaybill() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        givenEvidence(TestShipments.SHIPMENT_ID, EvidenceReference.WAYBILL);

        // when
        service.completeShipment(command(manager, "IRS-1"));

        // then
        assertThat(completions).singleElement().satisfies(completion -> {
            assertThat(completion.waybillNo()).isEqualTo("IRS-1");
            assertThat(completion.waybillEvidenceId()).isEqualTo(EVIDENCE);
            assertThat(completion.completedAt()).isEqualTo(NOW);
        });
    }

    @Test
    @DisplayName(
            "Tamamlama aynı akışta DISPATCH hareketini yazar ve varsayılan oranla AWAITING_FABRIC fire dosyasını açar")
    void shouldWriteDispatchAndOpenFireSettlement() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        givenEvidence(TestShipments.SHIPMENT_ID, EvidenceReference.WAYBILL);
        ledger.givenSentKg("1982.500");

        // when
        service.completeShipment(command(manager, "IRS-1"));

        // then
        assertThat(ledger.dispatchedShipments()).containsExactly(TestShipments.SHIPMENT_ID);
        assertThat(fireSettlements.find(TestShipments.SHIPMENT_ID)).get().satisfies(settlement -> {
            assertThat(settlement.status()).isEqualTo(FireSettlementStatus.AWAITING_FABRIC);
            assertThat(settlement.fireRate()).isEqualByComparingTo("0.0400");
            assertThat(settlement.sentKg()).isEqualByComparingTo("1982.500");
            assertThat(settlement.subcontractorId()).isEqualTo(TestShipments.SUBCONTRACTOR_ID);
        });
    }

    @Test
    @DisplayName("Geçersiz belgeyle tamamlama bakiye hareketi ve fire dosyası bırakmaz")
    void shouldNotWriteLedgerWhenEvidenceInvalid() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        CompleteShipmentCommand command = command(manager, "IRS-1");

        // when / then
        assertThatThrownBy(() -> service.completeShipment(command)).isInstanceOf(InvalidWaybillEvidenceException.class);
        assertThat(ledger.dispatchedShipments()).isEmpty();
        assertThat(fireSettlements.find(TestShipments.SHIPMENT_ID)).isEmpty();
    }

    @Test
    @DisplayName("Belge yoksa, başka sevkiyata aitse ya da irsaliye değilse tamamlanmaz")
    void shouldRejectInvalidEvidence() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        CompleteShipmentCommand command = command(manager, "IRS-1");

        // when / then
        assertThatThrownBy(() -> service.completeShipment(command)).isInstanceOf(InvalidWaybillEvidenceException.class);
        givenEvidence(UUID.randomUUID(), EvidenceReference.WAYBILL);
        assertThatThrownBy(() -> service.completeShipment(command)).isInstanceOf(InvalidWaybillEvidenceException.class);
        givenEvidence(TestShipments.SHIPMENT_ID, "DISPATCH_PHOTO");
        assertThatThrownBy(() -> service.completeShipment(command)).isInstanceOf(InvalidWaybillEvidenceException.class);
        assertThat(completions).isEmpty();
    }

    @Test
    @DisplayName("Aynı irsaliye numarasıyla tekrar gelen istek yeni kayıt üretmez")
    void shouldIgnoreRepeatedCompletion() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.COMPLETED));

        // when
        service.completeShipment(command(manager, TestShipments.WAYBILL_NO));

        // then
        assertThat(completions).isEmpty();
        assertThat(ledger.dispatchedShipments()).isEmpty();
        assertThat(fireSettlements.find(TestShipments.SHIPMENT_ID)).isEmpty();
    }
}
