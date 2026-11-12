package com.yarnshipment.evidence.adapter.in.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.in.GetEvidenceUseCase;
import com.yarnshipment.evidence.domain.model.EvidenceFile;
import com.yarnshipment.evidence.domain.model.EvidenceNotFoundException;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.moduleapi.evidence.EvidenceQueryApi.EvidenceSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class EvidenceQueryModuleAdapterTest {

    private final GetEvidenceUseCase getEvidence = mock(GetEvidenceUseCase.class);
    private final CurrentActorProvider actors = mock(CurrentActorProvider.class);
    private final EvidenceQueryModuleAdapter adapter = new EvidenceQueryModuleAdapter(getEvidence, actors);

    @Test
    @DisplayName("Görünen kanıtın özeti döner")
    void shouldReturnSummary() {
        Actor warehouse = TestTokens.actor("warehouse");
        EvidenceFile file = TestUploads.file(EvidenceType.WAYBILL);
        when(actors.getCurrentActor()).thenReturn(warehouse);
        when(getEvidence.getEvidence(any())).thenReturn(file);

        assertThat(adapter.findVisibleEvidence(file.id()))
                .contains(new EvidenceSummary(file.id(), file.shipmentId(), "WAYBILL", "VALID"));
    }

    @Test
    @DisplayName("Kanıt yok ya da kapsam dışıysa boş döner")
    void shouldReturnEmptyWhenHidden() {
        when(actors.getCurrentActor()).thenReturn(TestTokens.actor("warehouse"));
        when(getEvidence.getEvidence(any()))
                .thenThrow(new EvidenceNotFoundException(TestUploads.EVIDENCE_ID))
                .thenThrow(new AccessDeniedException("out of scope"));

        assertThat(adapter.findVisibleEvidence(TestUploads.EVIDENCE_ID)).isEmpty();
        assertThat(adapter.findVisibleEvidence(TestUploads.EVIDENCE_ID)).isEmpty();
    }

    @Test
    @DisplayName("Kanıt görme izni olmayan isteyen için kanıt okunmaz")
    void shouldNotReadWithoutPermission() {
        when(actors.getCurrentActor()).thenReturn(TestTokens.actor());

        assertThat(adapter.findVisibleEvidence(TestUploads.EVIDENCE_ID)).isEmpty();
        verifyNoInteractions(getEvidence);
    }
}
