package com.yarnshipment.evidence.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.evidence.TestUploads;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EvidenceInvalidationTest {

    private final EvidenceFile file = TestUploads.file(EvidenceType.DISPATCH_SCALE);

    @Test
    @DisplayName("Geçersiz kılma durumu ve gerekçeyi değiştirir; dosya ve hash yerinde kalır")
    void shouldInvalidateWithReason() {
        EvidenceFile invalidated = file.invalidate("  yanlış sevkiyat  ");

        assertThat(file.status()).isEqualTo(EvidenceStatus.VALID);
        assertThat(invalidated.status()).isEqualTo(EvidenceStatus.INVALIDATED);
        assertThat(invalidated.invalidatedReason()).isEqualTo("yanlış sevkiyat");
        assertThat(invalidated.stored()).isEqualTo(file.stored());
        assertThat(invalidated.storageKey()).isEqualTo(file.storageKey());
    }

    @Test
    @DisplayName("Gerekçesiz ya da ikinci kez geçersiz kılma reddedilir")
    void shouldRejectBlankReasonAndRepeat() {
        assertThatThrownBy(() -> file.invalidate("  ")).isInstanceOf(InvalidationReasonRequiredException.class);
        EvidenceFile invalidated = file.invalidate("hatalı");
        assertThatThrownBy(() -> invalidated.invalidate("tekrar"))
                .isInstanceOf(EvidenceAlreadyInvalidatedException.class);
    }
}
