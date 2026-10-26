package com.yarnshipment.discrepancy.application.port.out;

import com.yarnshipment.common.security.scope.AccessScope;
import com.yarnshipment.discrepancy.application.port.in.DiscrepancyPage;
import com.yarnshipment.discrepancy.application.port.in.ListDiscrepanciesQuery;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.model.DiscrepancyActivity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Uyuşmazlık okumaları. */
public interface DiscrepancyQueryPort {

    Optional<Discrepancy> findDiscrepancy(UUID discrepancyId);

    /** Kapsam WHERE koşuluna çevrilir; sonuçlar bellekte elenmez. */
    DiscrepancyPage findDiscrepancies(ListDiscrepanciesQuery.Filter filter, AccessScope scope, int page, int size);

    List<DiscrepancyActivity> findActivity(UUID discrepancyId);
}
