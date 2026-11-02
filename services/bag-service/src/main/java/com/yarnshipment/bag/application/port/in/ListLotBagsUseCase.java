package com.yarnshipment.bag.application.port.in;

import com.yarnshipment.bag.domain.model.LabeledBag;
import java.util.List;

/** Partinin çuvallarını etiket baskı sayılarıyla, en son oluşturulan önce getirir. */
public interface ListLotBagsUseCase {

    List<LabeledBag> listLotBags(ListLotBagsQuery query);
}
