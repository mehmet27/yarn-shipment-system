package com.yarnshipment.discrepancy.adapter.out.evidence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yarnshipment.discrepancy.application.port.out.EvidenceLookupPort.EvidenceRef;
import com.yarnshipment.discrepancy.domain.claim.EvidenceServiceUnavailableException;
import com.yarnshipment.moduleapi.ModuleUnavailableException;
import com.yarnshipment.moduleapi.evidence.EvidenceQueryApi;
import com.yarnshipment.moduleapi.evidence.EvidenceQueryApi.EvidenceSummary;
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
                .thenReturn(Optional.of(new EvidenceSummary(EVIDENCE, SHIPMENT, "QUALITY_CLAIM", "VALID")))
                .thenReturn(Optional.empty());

        assertThat(adapter.findEvidence(EVIDENCE)).contains(new EvidenceRef(EVIDENCE, SHIPMENT, "QUALITY_CLAIM"));
        assertThat(adapter.findEvidence(EVIDENCE)).isEmpty();
    }

    @Test
    @DisplayName("Modül yanıt vermezse kanıt doğrulanamaz")
    void shouldTranslateUnavailable() {
        when(api.findVisibleEvidence(EVIDENCE)).thenThrow(new ModuleUnavailableException("down"));

        assertThatThrownBy(() -> adapter.findEvidence(EVIDENCE))
                .isInstanceOf(EvidenceServiceUnavailableException.class);
    }
}
