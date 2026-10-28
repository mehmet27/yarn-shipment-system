package com.yarnshipment.evidence.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.OutOfScopeAccessException;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.in.RequestUploadCommand;
import com.yarnshipment.evidence.application.port.in.UploadTicket;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort.PresignedUpload;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort;
import com.yarnshipment.evidence.application.port.out.ShipmentAccessPort.ShipmentSnapshot;
import com.yarnshipment.evidence.application.port.out.UploadIntentPort;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.model.ShipmentLockedException;
import com.yarnshipment.evidence.domain.model.UploadIntent;
import com.yarnshipment.evidence.domain.model.UploadNotAllowedException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RequestUploadServiceTest {

    private static final Clock CLOCK = Clock.fixed(TestUploads.NOW, ZoneOffset.UTC);

    private final ShipmentAccessPort shipments = mock(ShipmentAccessPort.class);
    private final UploadIntentPort intents = mock(UploadIntentPort.class);
    private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
    private final RequestUploadService request =
            new RequestUploadService(shipments, intents, storage, new ScopeGuard(event -> {}, CLOCK), CLOCK);

    @Test
    @DisplayName("İstek, tür ve boyutu imzaya bağlar; bekleyen yüklemeyi isteyenin adına kaydeder")
    void shouldIssueTicketAndSaveIntent() {
        Actor warehouse = TestTokens.actor("warehouse");
        openShipment(false);
        when(storage.presignUpload(any()))
                .thenReturn(new PresignedUpload(
                        "https://s3/put", Map.of("content-type", "image/jpeg"), TestUploads.expiry()));

        UploadTicket ticket = request.requestUpload(command(warehouse, EvidenceType.DISPATCH_SCALE));

        assertThat(ticket.uploadUrl()).isEqualTo("https://s3/put");
        assertThat(ticket.expiresAt()).isEqualTo(TestUploads.expiry());
        ArgumentCaptor<UploadIntent> saved = ArgumentCaptor.forClass(UploadIntent.class);
        verify(intents).saveIntent(saved.capture());
        assertThat(saved.getValue().id()).isEqualTo(ticket.evidenceId());
        assertThat(saved.getValue().requestedBy()).isEqualTo(warehouse.userId());
        assertThat(saved.getValue().storageKey())
                .isEqualTo(UploadIntent.storageKeyOf(TestUploads.NOW, TestUploads.SHIPMENT_ID, ticket.evidenceId()));
    }

    @Test
    @DisplayName("Aşamasına izni olmayan rol, izinsiz tür ya da büyük dosya reddedilir; depoya hiç gidilmez")
    void shouldRejectDisallowedUploads() {
        Actor warehouse = TestTokens.actor("warehouse");
        Actor quality = TestTokens.actor("quality");

        assertThatThrownBy(() -> request.requestUpload(command(warehouse, EvidenceType.QUALITY_CLAIM)))
                .isInstanceOf(UploadNotAllowedException.class);
        assertThatThrownBy(() -> request.requestUpload(command(quality, EvidenceType.DISPATCH_SCALE)))
                .isInstanceOf(UploadNotAllowedException.class);
        UploadIntent.DeclaredFile zip = new UploadIntent.DeclaredFile(
                EvidenceType.DISPATCH_SCALE,
                "application/zip",
                TestUploads.SIZE,
                com.yarnshipment.evidence.domain.model.EvidenceSource.GALLERY,
                null);
        assertThatThrownBy(
                        () -> request.requestUpload(new RequestUploadCommand(warehouse, TestUploads.SHIPMENT_ID, zip)))
                .isInstanceOf(UploadNotAllowedException.class);
        verify(shipments, never()).findAccessible(any());
        verify(storage, never()).presignUpload(any());
    }

    @Test
    @DisplayName("Kilitli sevkiyata ve kapsam dışı lokasyona kanıt eklenemez")
    void shouldRejectLockedAndOutOfScopeShipments() {
        Actor warehouse = TestTokens.actor("warehouse");
        openShipment(true);
        assertThatThrownBy(() -> request.requestUpload(command(warehouse, EvidenceType.DISPATCH_SCALE)))
                .isInstanceOf(ShipmentLockedException.class);

        UUID otherSite = UUID.randomUUID();
        when(shipments.findAccessible(TestUploads.SHIPMENT_ID))
                .thenReturn(new ShipmentSnapshot(TestUploads.SHIPMENT_ID, otherSite, false, false));
        assertThatThrownBy(() -> request.requestUpload(command(warehouse, EvidenceType.DISPATCH_SCALE)))
                .isInstanceOf(OutOfScopeAccessException.class);
        verify(storage, never()).presignUpload(any());
    }

    @Test
    @DisplayName("Şoför, sahipliği shipment-service doğruladığı sevkiyata plaka fotoğrafı yükleyebilir")
    void shouldLetDriverUploadForVisibleShipment() {
        openShipment(false);
        when(storage.presignUpload(any())).thenReturn(new PresignedUpload("u", Map.of(), TestUploads.expiry()));

        UploadTicket ticket =
                request.requestUpload(command(TestTokens.actor(TestTokens.driverToken()), EvidenceType.VEHICLE_PLATE));

        assertThat(ticket.evidenceId()).isNotNull();
    }

    private void openShipment(boolean locked) {
        when(shipments.findAccessible(TestUploads.SHIPMENT_ID))
                .thenReturn(new ShipmentSnapshot(TestUploads.SHIPMENT_ID, TestTokens.SITE_ID, locked, false));
    }

    private static RequestUploadCommand command(Actor actor, EvidenceType type) {
        return new RequestUploadCommand(actor, TestUploads.SHIPMENT_ID, TestUploads.declared(type));
    }
}
