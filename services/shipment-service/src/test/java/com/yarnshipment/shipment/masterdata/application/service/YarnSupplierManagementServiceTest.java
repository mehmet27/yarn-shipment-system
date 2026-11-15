package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.out.YarnSupplierPort;
import com.yarnshipment.shipment.masterdata.domain.catalog.YarnSupplier;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class YarnSupplierManagementServiceTest {

    private final YarnSupplierPort suppliers = mock(YarnSupplierPort.class);
    private final YarnSupplierManagementService service = new YarnSupplierManagementService(suppliers);
    private final Actor manager = TestTokens.actor("manager");

    @Test
    @DisplayName("Tedarikçi adı temizlenerek eklenir")
    void shouldCreateWithCleanedName() {
        // given
        UUID id = UUID.randomUUID();
        when(suppliers.insertYarnSupplier(manager.userId(), "Şireci")).thenReturn(new YarnSupplier(id, "Şireci", true));

        // when
        YarnSupplier created = service.createYarnSupplier(manager, "  Şireci ");

        // then
        assertThat(created.id()).isEqualTo(id);
    }

    @Test
    @DisplayName("Olmayan tedarikçi güncellenemez ya da pasife alınamaz")
    void shouldRejectUnknownSupplier() {
        // given
        UUID id = UUID.randomUUID();
        when(suppliers.updateYarnSupplier(manager.userId(), id, "Karesi")).thenReturn(Optional.empty());
        when(suppliers.setYarnSupplierStatus(manager.userId(), id, MasterDataStatus.INACTIVE))
                .thenReturn(false);

        // when / then
        assertThatThrownBy(() -> service.updateYarnSupplier(manager, id, "Karesi"))
                .isInstanceOf(MasterDataNotFoundException.class);
        assertThatThrownBy(() -> service.changeYarnSupplierStatus(manager, id, MasterDataStatus.INACTIVE))
                .isInstanceOf(MasterDataNotFoundException.class);
        verify(suppliers).setYarnSupplierStatus(manager.userId(), id, MasterDataStatus.INACTIVE);
    }
}
