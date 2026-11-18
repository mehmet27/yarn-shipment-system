package com.yarnshipment.evidence.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort.ShipmentSnapshot;
import com.yarnshipment.evidence.domain.model.ScaleTicketNotOwnedException;
import com.yarnshipment.evidence.domain.model.ShipmentDepartedException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScaleTicketRefreshTest {

    private final EvidenceStorePort evidence = mock(EvidenceStorePort.class);
    private final ScaleTicketRefresh refresh = new ScaleTicketRefresh(evidence);
    private final Actor owner = TestTokens.actor("warehouse");
    private final Actor other = TestTokens.actor("warehouse");

    private static ShipmentSnapshot shipment(boolean departed) {
        return new ShipmentSnapshot(
                TestUploads.SHIPMENT_ID, TestTokens.SITE_ID, false, false, TestUploads.SHIPMENT_DATE, departed);
    }

    @Test
    @DisplayName("İlk kantar fişi, sevkiyat yolda olsa bile yüklenebilir (tartıdan hemen sonra gelir)")
    void shouldAllowFirstTicket() {
        when(evidence.findByShipment(TestUploads.SHIPMENT_ID)).thenReturn(List.of());

        assertThatCode(() -> refresh.require(owner, shipment(true))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Fiş varsa yenileme yalnızca yola çıkmamış sevkiyatta ve yalnızca ilk yükleyen tarafından yapılır")
    void shouldLetOnlyOwnerRefreshBeforeDeparture() {
        when(evidence.findByShipment(TestUploads.SHIPMENT_ID))
                .thenReturn(List.of(TestUploads.scaleTicketBy(owner.userId())));

        assertThatCode(() -> refresh.require(owner, shipment(false))).doesNotThrowAnyException();
        assertThatThrownBy(() -> refresh.require(owner, shipment(true))).isInstanceOf(ShipmentDepartedException.class);
        assertThatThrownBy(() -> refresh.require(other, shipment(false)))
                .isInstanceOf(ScaleTicketNotOwnedException.class);
    }
}
