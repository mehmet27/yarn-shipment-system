package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.out.MasterDataCommandPort;
import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import com.yarnshipment.shipment.masterdata.domain.model.DriverDetails;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import com.yarnshipment.shipment.masterdata.domain.model.Subcontractor;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MasterDataManagementServiceTest {

    private static final int TOO_LONG = 201;

    private final MasterDataCommandPort port = mock(MasterDataCommandPort.class);
    private final SubcontractorManagementService subcontractors = new SubcontractorManagementService(port);
    private final DriverManagementService drivers = new DriverManagementService(port);
    private final Actor actor = TestTokens.actor("admin");
    private final UUID id = UUID.randomUUID();

    @Test
    @DisplayName("Fasoncu ünvanı kenar boşluklarından arındırılıp kaydedilir")
    void shouldTrimTitleOnCreate() throws Exception {
        // given
        when(port.insertSubcontractor(actor.userId(), "Yeni")).thenReturn(new Subcontractor(id, "Yeni"));

        // when / then
        assertThat(subcontractors.createSubcontractor(actor, "  Yeni ").title()).isEqualTo("Yeni");
    }

    @Test
    @DisplayName("Boş ya da 200 karakteri aşan ünvan reddedilir; port çağrılmaz")
    void shouldRejectBlankAndTooLongTitles() throws Exception {
        assertThatThrownBy(() -> subcontractors.createSubcontractor(actor, "   "))
                .isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> subcontractors.createSubcontractor(actor, "x".repeat(TOO_LONG)))
                .isInstanceOf(InvalidMasterDataException.class);
        verify(port, never()).insertSubcontractor(actor.userId(), "");
    }

    @Test
    @DisplayName("Olmayan fasoncunun güncellenmesi ve pasife alınması bulunamadı hatası verir")
    void shouldFailWhenSubcontractorIsMissing() throws Exception {
        assertThatThrownBy(() -> subcontractors.updateSubcontractor(actor, id, "Yeni"))
                .isInstanceOf(MasterDataNotFoundException.class);
        assertThatThrownBy(() -> subcontractors.changeSubcontractorStatus(actor, id, MasterDataStatus.INACTIVE))
                .isInstanceOf(MasterDataNotFoundException.class);
    }

    @Test
    @DisplayName("Fasoncu güncellenir ve durumu değişir")
    void shouldUpdateAndDeactivateSubcontractor() throws Exception {
        // given
        when(port.updateSubcontractor(actor.userId(), id, "Yeni")).thenReturn(true);
        when(port.setSubcontractorStatus(actor.userId(), id, MasterDataStatus.INACTIVE))
                .thenReturn(true);

        // when / then
        assertThat(subcontractors.updateSubcontractor(actor, id, " Yeni ")).isEqualTo(new Subcontractor(id, "Yeni"));
        subcontractors.changeSubcontractorStatus(actor, id, MasterDataStatus.INACTIVE);
        verify(port).setSubcontractorStatus(actor.userId(), id, MasterDataStatus.INACTIVE);
    }

    @Test
    @DisplayName("Şoförün fasoncusu etkin değilse ya da yoksa ekleme ve güncelleme 400'dür")
    void shouldRequireActiveSubcontractorForDriver() throws Exception {
        // given
        DriverDetails details = new DriverDetails("Ali", id);
        when(port.activeSubcontractorExists(id)).thenReturn(false);

        // when / then
        assertThatThrownBy(() -> drivers.createDriver(actor, details)).isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> drivers.updateDriver(actor, id, details))
                .isInstanceOf(InvalidMasterDataException.class);
    }

    @Test
    @DisplayName("Fasoncusuz şoför kontrolsüz eklenir; fasoncusu etkinse bağlanır")
    void shouldCreateDriversWithAndWithoutSubcontractor() throws Exception {
        // given
        DriverDetails own = new DriverDetails(" Ali ", null);
        DriverDetails linked = new DriverDetails("Veli", id);
        when(port.insertDriver(actor.userId(), own)).thenReturn(new Driver(id, "Ali", null));
        when(port.activeSubcontractorExists(id)).thenReturn(true);
        when(port.insertDriver(actor.userId(), linked)).thenReturn(new Driver(id, "Veli", id));

        // when / then
        assertThat(drivers.createDriver(actor, own).subcontractorId()).isNull();
        assertThat(drivers.createDriver(actor, linked).subcontractorId()).isEqualTo(id);
        assertThat(own.fullName()).isEqualTo("Ali");
    }

    @Test
    @DisplayName("Şoför güncellenir, durumu değişir; olmayan şoför bulunamadı verir")
    void shouldUpdateDriverAndFailWhenMissing() throws Exception {
        // given
        DriverDetails details = new DriverDetails("Ali", null);
        assertThatThrownBy(() -> drivers.updateDriver(actor, id, details))
                .isInstanceOf(MasterDataNotFoundException.class);
        assertThatThrownBy(() -> drivers.changeDriverStatus(actor, id, MasterDataStatus.ACTIVE))
                .isInstanceOf(MasterDataNotFoundException.class);
        when(port.updateDriver(actor.userId(), id, details)).thenReturn(true);
        when(port.setDriverStatus(actor.userId(), id, MasterDataStatus.ACTIVE)).thenReturn(true);

        // when / then
        assertThat(drivers.updateDriver(actor, id, details)).isEqualTo(new Driver(id, "Ali", null));
        drivers.changeDriverStatus(actor, id, MasterDataStatus.ACTIVE);
        verify(port, times(2)).setDriverStatus(actor.userId(), id, MasterDataStatus.ACTIVE);
    }
}
