package com.yarnshipment.bag.application.port.out;

import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.LabeledBag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Çuval okuma portu. Çuvalın ayrıldığı sevkiyat varsa kayıtla birlikte gelir. */
public interface BagQueryPort {

    Optional<Bag> findByUniqueKey(String uniqueKey);

    List<Bag> findByUniqueKeys(Collection<String> uniqueKeys);

    List<Bag> findReservedFor(UUID shipmentId);

    /** Partinin çuvalları baskı sayılarıyla, en son oluşturulan önce, en fazla {@code limit} adet. */
    List<LabeledBag> findByLot(UUID lotId, int limit);
}
