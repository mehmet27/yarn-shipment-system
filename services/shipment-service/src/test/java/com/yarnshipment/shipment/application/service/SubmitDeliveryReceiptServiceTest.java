package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestDeliveries;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.SubmitDeliveryReceiptCommand;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubmitDeliveryReceiptServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T09:00:00Z");
    private static final UUID KEY = UUID.fromString("0192f0a0-0000-7000-8000-0000000000dd");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final RecordingDeliveryCommandPort commandPort = new RecordingDeliveryCommandPort();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final SubmitDeliveryReceiptService service = new SubmitDeliveryReceiptService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
            commandPort,
            evidenceId -> Optional.empty(),
            clock);
    private final Actor subcontractor = TestTokens.CONVERTER.toActor(TestTokens.subcontractorToken());

    @Test
    @DisplayName("Fasoncu kendi sevkiyatının tutanağını gönderir; gönderen token'daki kullanıcıdır")
    void shouldRecordDeliveryForOwnShipment() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));

        // when
        service.submitDeliveryReceipt(command(subcontractor));

        // then
        assertThat(commandPort.receipts()).singleElement().satisfies(receipt -> {
            assertThat(receipt.receivedBy()).isEqualTo(subcontractor.userId());
            assertThat(receipt.receivedAt()).isEqualTo(NOW);
        });
    }

    @Test
    @DisplayName("Gönderilmiş tutanağın tekrarı yeni kayıt oluşturmaz")
    void shouldIgnoreRepeatedSubmission() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        service.submitDeliveryReceipt(command(subcontractor));
        InMemoryShipmentQueryPort afterSubmit = new InMemoryShipmentQueryPort();
        afterSubmit.add(TestShipments.shipment(ShipmentStatus.CHECKING));
        SubmitDeliveryReceiptService retried = new SubmitDeliveryReceiptService(
                new ScopedShipmentLoader(afterSubmit, new ScopeGuard(event -> {}, clock)),
                commandPort,
                evidenceId -> Optional.empty(),
                clock);

        // when
        retried.submitDeliveryReceipt(command(subcontractor));

        // then
        assertThat(commandPort.receipts()).hasSize(1);
    }

    @Test
    @DisplayName("Yolda olan sevkiyata tutanak gönderilemez (409)")
    void shouldRejectBeforeArrival() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.IN_TRANSIT));

        // when / then
        assertThatThrownBy(() -> service.submitDeliveryReceipt(command(subcontractor)))
                .isInstanceOf(InvalidShipmentStatusException.class);
    }

    @Test
    @DisplayName("Başka fasoncunun sevkiyatına tutanak gönderilemez")
    void shouldRejectOtherSubcontractor() {
        // given
        queryPort.add(TestShipments.shipment(ShipmentStatus.AWAITING_DELIVERY));
        Actor other = TestTokens.CONVERTER.toActor(TestTokens.token("subcontractor")
                .claim("subcontractor_id", UUID.randomUUID().toString())
                .build());

        // when / then
        assertThatThrownBy(() -> service.submitDeliveryReceipt(command(other)))
                .isInstanceOf(OutOfScopeAccessException.class);
    }

    private static SubmitDeliveryReceiptCommand command(Actor actor) {
        return new SubmitDeliveryReceiptCommand(actor, TestShipments.SHIPMENT_ID, KEY, TestDeliveries.measurement());
    }
}
