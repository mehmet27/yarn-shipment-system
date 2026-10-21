package com.yarnshipment.shipment.masterdata.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.out.MasterDataQueryPort;
import com.yarnshipment.shipment.masterdata.domain.model.Site;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListMasterDataServiceTest {

    private static final int SIZE = 20;
    private static final int TOO_LARGE = 101;

    private final MasterDataQueryPort port = mock(MasterDataQueryPort.class);
    private final ScopeGuard scopeGuard = mock(ScopeGuard.class);
    private final ListMasterDataService service = new ListMasterDataService(port, scopeGuard);

    @Test
    @DisplayName("Lokasyon listesi isteyenin kapsamıyla sorgulanır")
    void shouldQuerySitesWithActorScope() throws Exception {
        // given
        Actor actor = TestTokens.actor("warehouse");
        AccessScope scope = new AccessScope(Set.of(UUID.randomUUID()), null, null, false);
        MasterDataQuery query = new MasterDataQuery(null, 0, SIZE);
        MasterDataPage<Site> page = new MasterDataPage<>(List.of(), 0, SIZE, 0);
        when(scopeGuard.scopeOf(actor)).thenReturn(scope);
        when(port.findSites(scope, query)).thenReturn(page);

        // when / then
        assertThat(service.listSites(actor, query)).isSameAs(page);
        verify(port).findSites(scope, query);
    }

    @Test
    @DisplayName("Sorgu sayfa boyu 1 ile 100 arasında olmalıdır")
    void shouldRejectInvalidPageSize() throws Exception {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new MasterDataQuery(null, 0, TOO_LARGE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
