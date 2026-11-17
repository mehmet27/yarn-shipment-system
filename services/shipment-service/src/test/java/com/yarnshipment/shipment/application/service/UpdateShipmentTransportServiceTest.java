package com.yarnshipment.shipment.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.transport.UpdateShipmentTransportCommand;
import com.yarnshipment.shipment.domain.model.InvalidNewShipmentException;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import com.yarnshipment.shipment.domain.transport.TransportAssignment;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateShipmentTransportServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");
    private static final UUID DRIVER = UUID.fromString("0192f0a0-0000-7000-8000-000000000d01");
    private static final UUID VEHICLE = UUID.fromString("0192f0a0-0000-7000-8000-000000000e01");

    private final List<String> written = new ArrayList<>();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private UpdateShipmentTransportService serviceFor(ShipmentStatus status) {
        InMemoryShipmentQueryPort queryPort = new InMemoryShipmentQueryPort();
        queryPort.add(TestShipments.shipment(status));
        return new UpdateShipmentTransportService(
                new ScopedShipmentLoader(queryPort, new ScopeGuard(event -> {}, clock)),
                (id, transport, actor, at) -> written.add(transport.driverName() + "/" + transport.plateNo()),
                clock);
    }

    private UpdateShipmentTransportCommand command(@Nullable String driverName, @Nullable String plateNo) {
        return new UpdateShipmentTransportCommand(
                TestTokens.actor("manager"),
                TestShipments.SHIPMENT_ID,
                new TransportAssignment(
                        driverName == null ? null : DRIVER,
                        driverName,
                        plateNo == null ? null : VEHICLE,
                        plateNo));
    }

    @Test
    @DisplayName("Müdür tamamlanmamış sevkiyatın şoför ve/veya aracını değiştirir; verilmeyen değişmez")
    void shouldUpdateOpenShipments() {
        for (ShipmentStatus status :
                List.of(ShipmentStatus.DRAFT, ShipmentStatus.WEIGHT_MISMATCH, ShipmentStatus.AWAITING_DELIVERY)) {
            serviceFor(status).updateShipmentTransport(command("Ali Veli", "34 ABC 123"));
        }
        serviceFor(ShipmentStatus.DRAFT).updateShipmentTransport(command("Ali Veli", null));

        assertThat(written)
                .containsExactly("Ali Veli/34 ABC 123", "Ali Veli/34 ABC 123", "Ali Veli/34 ABC 123", "Ali Veli/null");
    }

    @Test
    @DisplayName("Tamamlanmış ve iptal edilmiş sevkiyat değişmez")
    void shouldRejectClosedShipments() {
        for (ShipmentStatus status : List.of(ShipmentStatus.COMPLETED, ShipmentStatus.CANCELLED)) {
            UpdateShipmentTransportService service = serviceFor(status);
            assertThatThrownBy(() -> service.updateShipmentTransport(command("Ali Veli", "34 ABC 123")))
                    .isInstanceOf(InvalidShipmentStatusException.class);
        }
        assertThat(written).isEmpty();
    }

    @Test
    @DisplayName("Şoför kimliği adsız, araç kimliği plakasız verilemez; hiçbiri verilmeden istek olmaz")
    void shouldRequirePairs() {
        assertThatThrownBy(() -> new TransportAssignment(DRIVER, null, null, null))
                .isInstanceOf(InvalidNewShipmentException.class);
        assertThatThrownBy(() -> new TransportAssignment(null, null, VEHICLE, null))
                .isInstanceOf(InvalidNewShipmentException.class);
        assertThatThrownBy(() -> new TransportAssignment(null, null, null, null))
                .isInstanceOf(InvalidNewShipmentException.class);
    }
}
