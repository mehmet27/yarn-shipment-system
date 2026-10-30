package com.yarnshipment.evidence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.in.ListEvidenceQuery;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort.ShipmentSnapshot;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kanıt listesi, kör giriş ve kapsam kurallarıyla. */
class ListEvidenceServiceTest {

    private static final Clock CLOCK = Clock.fixed(TestUploads.NOW, ZoneOffset.UTC);

    private final EvidenceStorePort store = mock(EvidenceStorePort.class);
    private final ShipmentAccessPort shipments = mock(ShipmentAccessPort.class);
    private final ScopeGuard scopeGuard = new ScopeGuard(event -> {}, CLOCK);
    private final ListEvidenceService list = new ListEvidenceService(store, shipments, scopeGuard);
    private final EvidenceFile dispatchScale = TestUploads.file(EvidenceType.DISPATCH_SCALE);
    private final EvidenceFile signedDoc = TestUploads.file(EvidenceType.SIGNED_DELIVERY_DOC);

    @Test
    @DisplayName("Fasoncu karşılaştırmadan önce çıkış kantarını listede görmez; sonra görür")
    void shouldFilterByBlindEntry() {
        when(store.findByShipment(TestUploads.SHIPMENT_ID)).thenReturn(List.of(dispatchScale, signedDoc));
        var subcontractor = TestTokens.actor(TestTokens.subcontractorToken());
        var query = new ListEvidenceQuery(subcontractor, TestUploads.SHIPMENT_ID);

        snapshot(TestTokens.SITE_ID, false);
        assertThat(list.listEvidence(query)).containsExactly(signedDoc);

        snapshot(TestTokens.SITE_ID, true);
        assertThat(list.listEvidence(query)).containsExactly(dispatchScale, signedDoc);
    }

    @Test
    @DisplayName("Başka lokasyondaki sevkiyatın kanıtları listelenmez")
    void shouldRejectOutOfScope() {
        snapshot(UUID.randomUUID(), true);
        var query = new ListEvidenceQuery(TestTokens.actor("warehouse"), TestUploads.SHIPMENT_ID);

        assertThatThrownBy(() -> list.listEvidence(query)).isInstanceOf(OutOfScopeAccessException.class);
    }

    private void snapshot(UUID siteId, boolean comparisonDone) {
        when(shipments.findAccessible(TestUploads.SHIPMENT_ID))
                .thenReturn(new ShipmentSnapshot(TestUploads.SHIPMENT_ID, siteId, false, comparisonDone));
    }
}
