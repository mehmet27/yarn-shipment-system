package com.yarnshipment.bag.application.service;

import com.yarnshipment.bag.application.port.in.ListLockedLotsQuery;
import com.yarnshipment.bag.application.port.in.ListLockedLotsUseCase;
import com.yarnshipment.bag.application.port.out.LotQueryPort;
import com.yarnshipment.bag.domain.model.LockedLot;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kapsamdaki kilitli partileri listeler; kapsam sorguya eklenir, sonuç bellekte elenmez. */
@Service
public class ListLockedLotsService implements ListLockedLotsUseCase {

    private final LotQueryPort lots;

    public ListLockedLotsService(LotQueryPort lots) {
        this.lots = lots;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LockedLot> listLockedLots(ListLockedLotsQuery query) {
        return lots.findLockedInSites(query.actor().siteIds());
    }
}
