package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.out.VehicleSiteCommandPort;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidPlateException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataNotFoundException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataStatus;
import com.yarnshipment.shipment.masterdata.domain.model.Site;
import com.yarnshipment.shipment.masterdata.domain.model.Vehicle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VehicleSiteManagementServiceTest {

    private static final int TOO_LONG = 21;

    private final VehicleSiteCommandPort port = mock(VehicleSiteCommandPort.class);
    private final VehicleManagementService vehicles = new VehicleManagementService(port);
    private final SiteManagementService sites = new SiteManagementService(port);
    private final Actor actor = TestTokens.actor("admin");
    private final UUID id = UUID.randomUUID();

    @Test
    @DisplayName("Plaka boşluksuz büyük harfe çevrilip kaydedilir")
    void shouldNormalizePlate() throws Exception {
        // given
        when(port.insertVehicle(actor.userId(), "34ABC123")).thenReturn(new Vehicle(id, "34ABC123"));

        // when / then
        assertThat(vehicles.createVehicle(actor, " 34 abc 123 ").plateNo()).isEqualTo("34ABC123");
    }

    @Test
    @DisplayName("Boş plaka INVALID_MASTER_DATA, Türkiye plaka biçimine uymayan plaka INVALID_PLATE ile reddedilir")
    void shouldRejectBadPlates() throws Exception {
        assertThatThrownBy(() -> vehicles.createVehicle(actor, "  ")).isInstanceOf(InvalidMasterDataException.class);
        assertThatThrownBy(() -> vehicles.updateVehicle(actor, id, "A".repeat(TOO_LONG)))
                .isInstanceOf(InvalidPlateException.class);
        for (String plate : List.of("abc", "00ABC12", "82A1234", "34ABCD12", "34A1", "34ABC12345", "34-ABC-123")) {
            assertThatThrownBy(() -> vehicles.createVehicle(actor, plate))
                    .as(plate)
                    .isInstanceOf(InvalidPlateException.class);
        }
        verifyNoInteractions(port);
    }

    @Test
    @DisplayName("İl kodu 01-81, 1-3 harf ve 2-4 rakamlı plakalar kabul edilir")
    void shouldAcceptTurkishPlates() throws Exception {
        for (String plate : List.of("01A1234", "06 k 1234", "81AB123", "34 ABC 12")) {
            String normalized = plate.replace(" ", "").toUpperCase(Locale.ROOT);
            when(port.insertVehicle(actor.userId(), normalized)).thenReturn(new Vehicle(id, normalized));

            assertThat(vehicles.createVehicle(actor, plate).plateNo()).isEqualTo(normalized);
        }
    }

    @Test
    @DisplayName("Olmayan aracın güncellenmesi ve pasife alınması bulunamadı hatası verir")
    void shouldFailWhenVehicleIsMissing() throws Exception {
        assertThatThrownBy(() -> vehicles.updateVehicle(actor, id, "34ABC123"))
                .isInstanceOf(MasterDataNotFoundException.class);
        assertThatThrownBy(() -> vehicles.changeVehicleStatus(actor, id, MasterDataStatus.INACTIVE))
                .isInstanceOf(MasterDataNotFoundException.class);
    }

    @Test
    @DisplayName("Araç güncellenir ve durumu değişir")
    void shouldUpdateAndDeactivateVehicle() throws Exception {
        // given
        when(port.updateVehicle(actor.userId(), id, "06XYZ789")).thenReturn(true);
        when(port.setVehicleStatus(actor.userId(), id, MasterDataStatus.INACTIVE))
                .thenReturn(true);

        // when / then
        assertThat(vehicles.updateVehicle(actor, id, "06 xyz 789")).isEqualTo(new Vehicle(id, "06XYZ789"));
        vehicles.changeVehicleStatus(actor, id, MasterDataStatus.INACTIVE);
        verify(port).setVehicleStatus(actor.userId(), id, MasterDataStatus.INACTIVE);
    }

    @Test
    @DisplayName("Lokasyon adı kırpılıp kaydedilir; boş ad reddedilir")
    void shouldCreateSiteAndRejectBlankName() throws Exception {
        // given
        when(port.insertSite(actor.userId(), "Merkez")).thenReturn(new Site(id, "Merkez"));

        // when / then
        assertThat(sites.createSite(actor, " Merkez ").name()).isEqualTo("Merkez");
        assertThatThrownBy(() -> sites.createSite(actor, " ")).isInstanceOf(InvalidMasterDataException.class);
    }

    @Test
    @DisplayName("Lokasyon güncellenir, durumu değişir; olmayan lokasyon bulunamadı verir")
    void shouldUpdateSiteAndFailWhenMissing() throws Exception {
        // given
        assertThatThrownBy(() -> sites.updateSite(actor, id, "Yeni")).isInstanceOf(MasterDataNotFoundException.class);
        assertThatThrownBy(() -> sites.changeSiteStatus(actor, id, MasterDataStatus.ACTIVE))
                .isInstanceOf(MasterDataNotFoundException.class);
        when(port.updateSite(actor.userId(), id, "Yeni")).thenReturn(true);
        when(port.setSiteStatus(actor.userId(), id, MasterDataStatus.ACTIVE)).thenReturn(true);

        // when / then
        assertThat(sites.updateSite(actor, id, "Yeni")).isEqualTo(new Site(id, "Yeni"));
        sites.changeSiteStatus(actor, id, MasterDataStatus.ACTIVE);
    }
}
