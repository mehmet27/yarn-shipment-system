package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.bag.domain.model.LockedLot;
import java.util.List;

/** Çuval üretilebilecek partileri, en son kilitlenen önce getirir. */
public interface ListLockedLotsUseCase {

    List<LockedLot> listLockedLots(ListLockedLotsQuery query);
}
