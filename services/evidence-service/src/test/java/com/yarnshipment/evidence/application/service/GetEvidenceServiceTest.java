package com.yarnshipment.evidence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.in.GetEvidenceQuery;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort.ShipmentSnapshot;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceNotFoundException;
import com.yarnshipment.evidence.domain.model.ShipmentNotAccessibleException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GetEvidenceServiceTest {

    private static final Clock CLOCK = Clock.fixed(TestUploads.NOW, ZoneOffset.UTC);

    private final EvidenceStorePort store = mock(EvidenceStorePort.class);
    private final ShipmentAccessPort shipments = mock(ShipmentAccessPort.class);
    private final GetEvidenceService service = new GetEvidenceService(store, shipments, new ScopeGuard(e -> {}, CLOCK));
    private final Actor warehouse = TestTokens.actor("warehouse");
    private final EvidenceFile file =
            EvidenceFile.confirm(TestUploads.intent(UUID.randomUUID()), TestUploads.stored(), TestUploads.NOW);

    @Test
    @DisplayName("Sevkiyatı görünen kullanıcı kanıt üst verisini okur")
    void shouldReturnEvidence() {
        when(store.findEvidence(file.id())).thenReturn(Optional.of(file));
        when(shipments.findAccessible(file.shipmentId()))
                .thenReturn(new ShipmentSnapshot(file.shipmentId(), TestTokens.SITE_ID, false, false));

        assertThat(service.getEvidence(new GetEvidenceQuery(warehouse, file.id())))
                .isEqualTo(file);
    }

    @Test
    @DisplayName("Kanıt yoksa, sevkiyat görünmüyorsa ya da izin yoksa aynı şekilde bulunamadı döner")
    void shouldHideEvidence() {
        GetEvidenceQuery query = new GetEvidenceQuery(warehouse, file.id());
        assertThatThrownBy(() -> service.getEvidence(query)).isInstanceOf(EvidenceNotFoundException.class);

        when(store.findEvidence(file.id())).thenReturn(Optional.of(file));
        when(shipments.findAccessible(file.shipmentId()))
                .thenThrow(new ShipmentNotAccessibleException(file.shipmentId()));
        assertThatThrownBy(() -> service.getEvidence(query)).isInstanceOf(EvidenceNotFoundException.class);

        GetEvidenceQuery noPermission = new GetEvidenceQuery(TestTokens.actor("nobody"), file.id());
        assertThatThrownBy(() -> service.getEvidence(noPermission)).isInstanceOf(EvidenceNotFoundException.class);
    }

    @Test
    @DisplayName("Başka lokasyondaki sevkiyatın kanıtı kapsam dışıdır")
    void shouldRejectOutOfScope() {
        when(store.findEvidence(file.id())).thenReturn(Optional.of(file));
        when(shipments.findAccessible(file.shipmentId()))
                .thenReturn(new ShipmentSnapshot(file.shipmentId(), UUID.randomUUID(), false, false));

        assertThatThrownBy(() -> service.getEvidence(new GetEvidenceQuery(warehouse, file.id())))
                .isInstanceOf(OutOfScopeAccessException.class);
    }
}
