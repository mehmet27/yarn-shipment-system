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
import com.yarnshipment.evidence.application.port.in.ConfirmUploadCommand;
import com.yarnshipment.evidence.application.port.out.EvidenceStorePort;
import com.yarnshipment.evidence.application.port.out.ObjectStoragePort;
import com.yarnshipment.evidence.application.port.out.UploadIntentPort;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.StoredObject;
import com.yarnshipment.evidence.domain.model.UploadIntentNotFoundException;
import com.yarnshipment.evidence.domain.model.UploadMismatchException;
import com.yarnshipment.evidence.domain.model.UploadNotCompletedException;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfirmUploadServiceTest {

    private static final Clock CLOCK = Clock.fixed(TestUploads.NOW, ZoneOffset.UTC);

    private final UploadIntentPort intents = mock(UploadIntentPort.class);
    private final ObjectStoragePort storage = mock(ObjectStoragePort.class);
    private final EvidenceStorePort evidence = mock(EvidenceStorePort.class);
    private final ConfirmUploadService confirm =
            new ConfirmUploadService(intents, storage, evidence, new ScopeGuard(event -> {}, CLOCK), CLOCK);

    @Test
    @DisplayName("Onay, depodan okunan gerçek özelliklerle kanıtı kaydeder")
    void shouldConfirmUpload() {
        Actor warehouse = TestTokens.actor("warehouse");
        when(intents.findIntent(TestUploads.EVIDENCE_ID))
                .thenReturn(Optional.of(TestUploads.intent(warehouse.userId())));
        when(evidence.findEvidence(TestUploads.EVIDENCE_ID)).thenReturn(Optional.empty());
        when(storage.inspect(any())).thenReturn(Optional.of(TestUploads.stored()));

        EvidenceFile file = confirm.confirmUpload(new ConfirmUploadCommand(warehouse, TestUploads.EVIDENCE_ID));

        assertThat(file.stored().sha256()).isEqualTo(TestUploads.SHA);
        verify(evidence).saveEvidence(file);
    }

    @Test
    @DisplayName("Aynı onay tekrarlanırsa kayıtlı kanıt döner, ikinci kayıt yazılmaz")
    void shouldBeIdempotent() {
        Actor warehouse = TestTokens.actor("warehouse");
        EvidenceFile existing =
                EvidenceFile.confirm(TestUploads.intent(warehouse.userId()), TestUploads.stored(), TestUploads.NOW);
        when(intents.findIntent(TestUploads.EVIDENCE_ID))
                .thenReturn(Optional.of(TestUploads.intent(warehouse.userId())));
        when(evidence.findEvidence(TestUploads.EVIDENCE_ID)).thenReturn(Optional.of(existing));

        assertThat(confirm.confirmUpload(new ConfirmUploadCommand(warehouse, TestUploads.EVIDENCE_ID)))
                .isEqualTo(existing);
        verify(evidence, never()).saveEvidence(any());
        verify(storage, never()).inspect(any());
    }

    @Test
    @DisplayName("Başkasının isteğini onaylayamazsın; dosya yoksa ya da bildirilenle uyuşmuyorsa onay reddedilir")
    void shouldRejectInvalidConfirmations() {
        Actor warehouse = TestTokens.actor("warehouse");
        ConfirmUploadCommand command = new ConfirmUploadCommand(warehouse, TestUploads.EVIDENCE_ID);
        when(intents.findIntent(TestUploads.EVIDENCE_ID))
                .thenReturn(Optional.of(TestUploads.intent(UUID.randomUUID())));
        assertThatThrownBy(() -> confirm.confirmUpload(command)).isInstanceOf(UploadIntentNotFoundException.class);

        when(intents.findIntent(TestUploads.EVIDENCE_ID))
                .thenReturn(Optional.of(TestUploads.intent(warehouse.userId())));
        when(evidence.findEvidence(TestUploads.EVIDENCE_ID)).thenReturn(Optional.empty());
        when(storage.inspect(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> confirm.confirmUpload(command)).isInstanceOf(UploadNotCompletedException.class);

        when(storage.inspect(any())).thenReturn(Optional.of(new StoredObject(1, TestUploads.SHA, TestUploads.JPEG)));
        assertThatThrownBy(() -> confirm.confirmUpload(command)).isInstanceOf(UploadMismatchException.class);
        verify(evidence, never()).saveEvidence(any());
    }
}
