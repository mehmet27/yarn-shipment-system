package com.yarnshipment.evidence.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.evidence.TestUploads;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UploadModelTest {

    private static final UUID USER = UUID.fromString("0192f0a0-0000-7000-8000-0000000000a1");

    @Test
    @DisplayName("Onay, depodaki gerçek özelliklerle kanıt kaydı üretir; hash sunucunun hesapladığıdır")
    void shouldConfirmMatchingFile() {
        EvidenceFile file = EvidenceFile.confirm(TestUploads.intent(USER), TestUploads.stored(), TestUploads.NOW);

        assertThat(file.id()).isEqualTo(TestUploads.EVIDENCE_ID);
        assertThat(file.stored().sha256()).isEqualTo(TestUploads.SHA);
        assertThat(file.uploadedBy()).isEqualTo(USER);
        assertThat(file.storageKey()).isEqualTo("evidence/2026/10/" + TestUploads.SHIPMENT_ID + "/" + file.id());
    }

    @Test
    @DisplayName("Boyutu ya da türü bildirilenden farklı dosya reddedilir")
    void shouldRejectMismatch() {
        UploadIntent intent = TestUploads.intent(USER);

        assertThatThrownBy(() -> EvidenceFile.confirm(
                        intent,
                        new StoredObject(TestUploads.SIZE + 1, TestUploads.SHA, TestUploads.JPEG),
                        TestUploads.NOW))
                .isInstanceOf(UploadMismatchException.class);
        assertThatThrownBy(() -> EvidenceFile.confirm(
                        intent, new StoredObject(TestUploads.SIZE, TestUploads.SHA, "image/png"), TestUploads.NOW))
                .isInstanceOf(UploadMismatchException.class);
    }

    @Test
    @DisplayName("Dosya sınırları: izinli türler ve 20 MB üst sınır")
    void shouldApplyUploadRules() {
        assertThat(UploadRules.isAllowedMimeType("application/pdf")).isTrue();
        assertThat(UploadRules.isAllowedMimeType("application/zip")).isFalse();
        assertThat(UploadRules.isAllowedSize(UploadRules.MAX_SIZE_BYTES)).isTrue();
        assertThat(UploadRules.isAllowedSize(UploadRules.MAX_SIZE_BYTES + 1)).isFalse();
        assertThat(UploadRules.isAllowedSize(0)).isFalse();
    }

    @Test
    @DisplayName("Mühür hem sevk hem teslim aşamasında yüklenir; imzalı belge yalnızca teslimde")
    void shouldMapTypesToStages() {
        assertThat(EvidenceType.SEAL.stages()).containsExactly(UploadStage.DISPATCH, UploadStage.DELIVERY);
        assertThat(EvidenceType.SIGNED_DELIVERY_DOC.stages()).containsExactly(UploadStage.DELIVERY);
        assertThat(EvidenceType.VEHICLE_PLATE.stages()).containsExactly(UploadStage.DRIVER);
        assertThat(EvidenceType.LAB_RESULT.stages()).containsExactly(UploadStage.CLAIM);
        assertThat(EvidenceType.BAG_LABEL.stages()).containsExactly(UploadStage.DISPATCH);
    }
}
