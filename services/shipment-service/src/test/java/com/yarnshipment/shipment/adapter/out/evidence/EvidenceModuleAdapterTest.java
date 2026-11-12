package com.yarnshipment.shipment.adapter.out.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yarnshipment.moduleapi.ModuleUnavailableException;
import com.yarnshipment.moduleapi.evidence.EvidenceQueryApi;
import com.yarnshipment.moduleapi.evidence.EvidenceQueryApi.EvidenceSummary;
import com.yarnshipment.shipment.domain.evidence.EvidenceReference;
import com.yarnshipment.shipment.domain.evidence.EvidenceServiceUnavailableException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EvidenceModuleAdapterTest {

    private static final UUID EVIDENCE = UUID.fromString("0192f0a0-0000-7000-8000-0000000c0003");
    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000301");

    private final EvidenceQueryApi api = mock(EvidenceQueryApi.class);
    private final EvidenceModuleAdapter adapter = new EvidenceModuleAdapter(api);

    @Test
    @DisplayName("Modül özeti kanıt başvurusuna çevrilir; görünmeyen kanıt boş döner")
    void shouldMapSummaryAndKeepEmpty() {
        when(api.findVisibleEvidence(EVIDENCE))
                .thenReturn(Optional.of(new EvidenceSummary(EVIDENCE, SHIPMENT, "WAYBILL", "VALID")))
                .thenReturn(Optional.empty());

        assertThat(adapter.findEvidence(EVIDENCE))
                .contains(
                        new EvidenceReference(EVIDENCE, SHIPMENT, EvidenceReference.WAYBILL, EvidenceReference.VALID));
        assertThat(adapter.findEvidence(EVIDENCE)).isEmpty();
    }

    @Test
    @DisplayName("Modül yanıt vermezse kanıt servisi hatası fırlatılır")
    void shouldTranslateUnavailable() {
        when(api.findVisibleEvidence(EVIDENCE)).thenThrow(new ModuleUnavailableException("down"));

        assertThatThrownBy(() -> adapter.findEvidence(EVIDENCE))
                .isInstanceOf(EvidenceServiceUnavailableException.class);
    }
}
