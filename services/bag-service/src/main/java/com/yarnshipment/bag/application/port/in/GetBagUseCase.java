package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.bag.domain.model.Bag;

/** Çuvalı Unique Key ile getirir; çuvalın lokasyonu isteyenin kapsamında olmalıdır. */
public interface GetBagUseCase {

    Bag getBag(GetBagQuery query);
}
