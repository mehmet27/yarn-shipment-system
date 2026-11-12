package com.yarnshipment.evidence.adapter.out.shipment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort.ShipmentSnapshot;
import com.yarnshipment.evidence.domain.model.ShipmentNotAccessibleException;
import com.yarnshipment.evidence.domain.model.ShipmentServiceUnavailableException;
import com.yarnshipment.moduleapi.ModuleUnavailableException;
import com.yarnshipment.moduleapi.shipment.ShipmentAccessApi;
import com.yarnshipment.moduleapi.shipment.ShipmentAccessApi.ShipmentSummary;
import com.yarnshipment.moduleapi.shipment.ShipmentNotVisibleException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShipmentModuleAdapterTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");
    private static final UUID SITE = UUID.fromString("0192f0a0-0000-7000-8000-000000000101");

    private final ShipmentAccessApi api = mock(ShipmentAccessApi.class);
    private final ShipmentModuleAdapter adapter = new ShipmentModuleAdapter(api);

    @Test
    @DisplayName("Durum, kilit ve tamamlanma bilgisine çevrilir")
    void shouldDeriveLockedAndCompleted() {
        when(api.findVisibleShipment(SHIPMENT))
                .thenReturn(new ShipmentSummary(SHIPMENT, SITE, "COMPLETED"))
                .thenReturn(new ShipmentSummary(SHIPMENT, SITE, "CANCELLED"))
                .thenReturn(new ShipmentSummary(SHIPMENT, SITE, "IN_TRANSIT"));

        assertThat(adapter.findAccessible(SHIPMENT)).isEqualTo(new ShipmentSnapshot(SHIPMENT, SITE, true, true));
        assertThat(adapter.findAccessible(SHIPMENT)).isEqualTo(new ShipmentSnapshot(SHIPMENT, SITE, true, false));
        assertThat(adapter.findAccessible(SHIPMENT)).isEqualTo(new ShipmentSnapshot(SHIPMENT, SITE, false, false));
    }

    @Test
    @DisplayName("Görünmeyen sevkiyat ve ulaşılamayan modül kendi hatalarına çevrilir")
    void shouldTranslateFailures() {
        when(api.findVisibleShipment(SHIPMENT))
                .thenThrow(new ShipmentNotVisibleException(SHIPMENT))
                .thenThrow(new ModuleUnavailableException("down"));

        assertThatThrownBy(() -> adapter.findAccessible(SHIPMENT)).isInstanceOf(ShipmentNotAccessibleException.class);
        assertThatThrownBy(() -> adapter.findAccessible(SHIPMENT))
                .isInstanceOf(ShipmentServiceUnavailableException.class);
    }
}
