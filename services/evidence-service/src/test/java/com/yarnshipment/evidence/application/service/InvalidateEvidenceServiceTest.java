package com.yarnshipment.evidence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.in.InvalidateEvidenceCommand;
import com.yarnshipment.evidence.application.port.out.EvidenceInvalidationPort;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort.ShipmentSnapshot;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceNotFoundException;
import com.yarnshipment.evidence.domain.model.EvidenceStatus;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.InvalidationReasonRequiredException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InvalidateEvidenceServiceTest {

    private static final Clock CLOCK = Clock.fixed(TestUploads.NOW, ZoneOffset.UTC);

    private final EvidenceStorePort store = mock(EvidenceStorePort.class);
    private final EvidenceInvalidationPort invalidations = mock(EvidenceInvalidationPort.class);
    private final ShipmentAccessPort shipments = mock(ShipmentAccessPort.class);
    private final InvalidateEvidenceService service =
            new InvalidateEvidenceService(store, invalidations, shipments, new ScopeGuard(event -> {}, CLOCK), CLOCK);
    private final EvidenceFile file = TestUploads.file(EvidenceType.DISPATCH_SCALE);
    private final Actor admin = TestTokens.actor("admin");

    @Test
    @DisplayName("Admin kanıtı gerekçeyle geçersiz kılar ve kayıt yazılır")
    void shouldInvalidate() {
        when(store.findEvidence(file.id())).thenReturn(Optional.of(file));
        when(shipments.findAccessible(file.shipmentId()))
                .thenReturn(new ShipmentSnapshot(file.shipmentId(), TestTokens.SITE_ID, false, false));

        EvidenceFile result = service.invalidateEvidence(new InvalidateEvidenceCommand(admin, file.id(), "yanlış"));

        assertThat(result.status()).isEqualTo(EvidenceStatus.INVALIDATED);
        verify(invalidations).saveInvalidation(result, admin.userId(), TestUploads.NOW);
    }

    @Test
    @DisplayName("Admin olmayan için kanıt yoktur; gerekçesiz istek yazılmaz")
    void shouldHideFromOthersAndRequireReason() {
        when(store.findEvidence(file.id())).thenReturn(Optional.of(file));
        when(shipments.findAccessible(file.shipmentId()))
                .thenReturn(new ShipmentSnapshot(file.shipmentId(), TestTokens.SITE_ID, false, false));
        InvalidateEvidenceCommand byWarehouse =
                new InvalidateEvidenceCommand(TestTokens.actor("warehouse"), file.id(), "x");
        InvalidateEvidenceCommand blank = new InvalidateEvidenceCommand(admin, file.id(), " ");

        assertThatThrownBy(() -> service.invalidateEvidence(byWarehouse)).isInstanceOf(EvidenceNotFoundException.class);
        assertThatThrownBy(() -> service.invalidateEvidence(blank))
                .isInstanceOf(InvalidationReasonRequiredException.class);
        verify(invalidations, never()).saveInvalidation(any(), any(), any());
    }
}
