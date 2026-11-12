package com.yarnshipment.shipment.adapter.in.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.moduleapi.shipment.ShipmentAccessApi.ShipmentSummary;
import com.yarnshipment.moduleapi.shipment.ShipmentNotVisibleException;
import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.GetShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.ShipmentAudience;
import com.yarnshipment.shipment.application.port.in.ShipmentDetails;
import com.yarnshipment.shipment.application.port.in.ShipmentNotFoundException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ShipmentAccessModuleAdapterTest {

    private final GetShipmentUseCase getShipment = mock(GetShipmentUseCase.class);
    private final CurrentActorProvider actors = mock(CurrentActorProvider.class);
    private final ShipmentAccessModuleAdapter adapter = new ShipmentAccessModuleAdapter(getShipment, actors);
    private final Actor warehouse = TestTokens.actor("warehouse");

    @Test
    @DisplayName("Görünen sevkiyatın özeti döner")
    void shouldReturnSummaryOfVisibleShipment() {
        when(actors.getCurrentActor()).thenReturn(warehouse);
        when(getShipment.getShipment(any()))
                .thenReturn(new ShipmentDetails(
                        TestShipments.shipment(ShipmentStatus.COMPLETED), ShipmentAudience.WAREHOUSE, Set.of()));

        ShipmentSummary summary = adapter.findVisibleShipment(TestShipments.SHIPMENT_ID);

        assertThat(summary)
                .isEqualTo(new ShipmentSummary(TestShipments.SHIPMENT_ID, TestShipments.SITE_ID, "COMPLETED"));
    }

    @Test
    @DisplayName("Yok ya da kapsam dışı sevkiyat görünmez sayılır")
    void shouldHideMissingOrOutOfScopeShipment() {
        when(actors.getCurrentActor()).thenReturn(warehouse);
        when(getShipment.getShipment(any()))
                .thenThrow(new ShipmentNotFoundException(TestShipments.SHIPMENT_ID))
                .thenThrow(new AccessDeniedException("out of scope"));

        assertThatThrownBy(() -> adapter.findVisibleShipment(TestShipments.SHIPMENT_ID))
                .isInstanceOf(ShipmentNotVisibleException.class);
        assertThatThrownBy(() -> adapter.findVisibleShipment(TestShipments.SHIPMENT_ID))
                .isInstanceOf(ShipmentNotVisibleException.class);
    }
}
