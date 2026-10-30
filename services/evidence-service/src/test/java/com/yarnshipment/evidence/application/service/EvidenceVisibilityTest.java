package com.yarnshipment.evidence.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kör giriş: kim hangi kanıt türünü, karşılaştırma öncesi ve sonrası görür. */
class EvidenceVisibilityTest {

    private final Actor warehouse = TestTokens.actor("warehouse");
    private final Actor subcontractor = TestTokens.actor(TestTokens.subcontractorToken());
    private final Actor driver = TestTokens.actor(TestTokens.driverToken());

    @Test
    @DisplayName("Depo çıkış fotoğraflarını hep, teslim fotoğraflarını yalnız karşılaştırmadan sonra görür")
    void shouldBlindWarehouseFromDeliveryEvidence() {
        assertThat(EvidenceVisibility.mayView(warehouse, EvidenceType.DISPATCH_SCALE, false))
                .isTrue();
        assertThat(EvidenceVisibility.mayView(warehouse, EvidenceType.DELIVERY_SCALE, false))
                .isFalse();
        assertThat(EvidenceVisibility.mayView(warehouse, EvidenceType.DELIVERY_SCALE, true))
                .isTrue();
    }

    @Test
    @DisplayName("Fasoncu teslim ve şikâyet kanıtlarını hep, çıkış kantarını yalnız karşılaştırmadan sonra görür")
    void shouldBlindSubcontractorFromDispatchEvidence() {
        assertThat(EvidenceVisibility.mayView(subcontractor, EvidenceType.SIGNED_DELIVERY_DOC, false))
                .isTrue();
        assertThat(EvidenceVisibility.mayView(subcontractor, EvidenceType.QUALITY_CLAIM, false))
                .isTrue();
        assertThat(EvidenceVisibility.mayView(subcontractor, EvidenceType.DISPATCH_SCALE, false))
                .isFalse();
        assertThat(EvidenceVisibility.mayView(subcontractor, EvidenceType.SEAL, false))
                .isFalse();
        assertThat(EvidenceVisibility.mayView(subcontractor, EvidenceType.DISPATCH_SCALE, true))
                .isTrue();
    }

    @Test
    @DisplayName("Şoför yalnızca plaka fotoğrafını görür; kalite, yönetici ve admin hepsini görür")
    void shouldLimitDriverAndOpenForOversightRoles() {
        assertThat(EvidenceVisibility.mayView(driver, EvidenceType.VEHICLE_PLATE, false))
                .isTrue();
        assertThat(EvidenceVisibility.mayView(driver, EvidenceType.DISPATCH_SCALE, true))
                .isFalse();
        for (String role : new String[] {"quality", "manager", "admin"}) {
            assertThat(EvidenceVisibility.mayView(TestTokens.actor(role), EvidenceType.DELIVERY_SCALE, false))
                    .isTrue();
        }
    }
}
