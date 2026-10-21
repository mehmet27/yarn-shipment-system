package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestDispatches;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.RecordDispatchCommand;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecordDispatchServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T09:00:00Z");
    private static final UUID KEY = UUID.fromString("0192f0a0-0000-7000-8000-0000000000cc");

    private final InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
    private final RecordingDispatchCommandPort commandPort = new RecordingDispatchCommandPort();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final RecordDispatchService service = new RecordDispatchService(
            new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)), commandPort, clock);
    private final Actor warehouse = TestTokens.actor("warehouse");

    private RecordDispatchCommand command(Actor actor) {
        return new RecordDispatchCommand(actor, TestShipments.SHIPMENT_ID, KEY, TestDispatches.measurement());
    }

    @Test
    @DisplayName("Depo çalışanı kendi lokasyonundaki taslak sevkiyatın çıkışını sunucu saatiyle kaydeder")
    void shouldRecordDispatchForWarehouse() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));

        service.recordDispatch(command(warehouse));

        assertThat(commandPort.recordings()).singleElement().satisfies(recording -> {
            assertThat(recording.recordedAt()).isEqualTo(NOW);
            assertThat(recording.sealNo()).isEqualTo("M-0001");
        });
    }

    @Test
    @DisplayName("Aynı çıkış kaydının tekrarı yeni kayıt oluşturmaz ve hata vermez")
    void shouldIgnoreRepeatedDispatch() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.LOADED));
        commandPort.addExisting(TestShipments.shipment(ShipmentStatus.DRAFT)
                .recordDispatch(command(warehouse).measurement(), NOW));

        service.recordDispatch(command(warehouse));

        assertThat(commandPort.recordings()).hasSize(1);
    }

    @Test
    @DisplayName("Kaydı olmayan ve taslak olmayan sevkiyatta 409 hatası verilir")
    void shouldRejectDispatchOutsideDraft() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.IN_TRANSIT));

        assertThatThrownBy(() -> service.recordDispatch(command(warehouse)))
                .isInstanceOf(InvalidShipmentStatusException.class);
        assertThat(commandPort.recordings()).isEmpty();
    }

    @Test
    @DisplayName("Başka lokasyonun deposu kapsam dışıdır")
    void shouldRejectWarehouseOfOtherSite() {
        queryPort.add(TestShipments.shipment(ShipmentStatus.DRAFT));
        Actor otherSite = TestTokens.CONVERTER.toActor(TestTokens.token("warehouse")
                .claim("site_ids", List.of(UUID.randomUUID().toString()))
                .build());

        assertThatThrownBy(() -> service.recordDispatch(command(otherSite)))
                .isInstanceOf(OutOfScopeAccessException.class);
        assertThat(commandPort.recordings()).isEmpty();
    }

    @Test
    @DisplayName("Olmayan sevkiyat 404 verir")
    void shouldRejectUnknownShipment() {
        assertThatThrownBy(() -> service.recordDispatch(command(warehouse)))
                .isInstanceOf(ShipmentNotFoundException.class);
    }
}
