package com.yarnshipment.audit.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.audit.application.port.out.AuditChainPort;
import com.yarnshipment.audit.application.port.out.AuditQueryPort;
import java.util.OptionalLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerifyAuditChainServiceTest {

    private static final long LAST_VERIFIED = 10L;
    private static final long LATEST = 15L;

    private final AuditQueryPort queries = mock(AuditQueryPort.class);
    private final AuditChainPort chain = mock(AuditChainPort.class);
    private final VerifyAuditChainService service = new VerifyAuditChainService(queries, chain);

    @Test
    @DisplayName("Son doğrulanan kayıttan sonraki aralık doğrulanır")
    void shouldVerifyOnlyNewRecords() {
        when(queries.lastVerifiedId()).thenReturn(LAST_VERIFIED);
        when(queries.latestRecordId()).thenReturn(LATEST);
        when(chain.verify(LAST_VERIFIED + 1, LATEST)).thenReturn(OptionalLong.empty());

        assertThat(service.verifyChain()).isEmpty();

        verify(chain).verify(LAST_VERIFIED + 1, LATEST);
    }

    @Test
    @DisplayName("Bozuk kaydın sırası çağırana döner")
    void shouldReturnBrokenRecord() {
        when(queries.lastVerifiedId()).thenReturn(0L);
        when(queries.latestRecordId()).thenReturn(LATEST);
        when(chain.verify(1L, LATEST)).thenReturn(OptionalLong.of(LAST_VERIFIED));

        assertThat(service.verifyChain()).hasValue(LAST_VERIFIED);
    }

    @Test
    @DisplayName("Doğrulanacak yeni kayıt yoksa zincire dokunulmaz")
    void shouldSkipWhenNothingIsNew() {
        when(queries.lastVerifiedId()).thenReturn(LATEST);
        when(queries.latestRecordId()).thenReturn(LATEST);

        assertThat(service.verifyChain()).isEmpty();

        verifyNoInteractions(chain);
    }
}
