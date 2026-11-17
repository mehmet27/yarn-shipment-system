package com.yarnshipment.shipment.domain.transport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransportAssignmentTest {

    @Test
    @DisplayName("toString şoför adını (kişisel veri) göstermez")
    void shouldMaskDriverName() {
        var assignment = new TransportAssignment(UUID.randomUUID(), "Ali Veli", null, null);

        assertThat(assignment.toString()).contains("driverName=***").doesNotContain("Ali Veli");
    }

    @Test
    @DisplayName("Yalnızca araç verilebilir")
    void shouldAllowVehicleOnly() {
        var vehicleId = UUID.randomUUID();
        var assignment = new TransportAssignment(null, null, vehicleId, "34ABC123");

        assertThat(assignment.vehicleId()).isEqualTo(vehicleId);
        assertThat(assignment.driverId()).isNull();
    }
}
