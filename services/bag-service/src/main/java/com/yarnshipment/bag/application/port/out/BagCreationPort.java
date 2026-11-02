package com.yarnshipment.bag.application.port.out;

import com.yarnshipment.bag.domain.model.Bag;
import com.yarnshipment.bag.domain.model.LockedLot;
import com.yarnshipment.bag.domain.model.PackageType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Çuval yazma portu; Unique Key adaptörde, tahmin edilemeyen ve tekil biçimde üretilir. */
public interface BagCreationPort {

    /** Partiye {@code count} çuval ekler ve oluşturulan çuvalları döner. */
    List<Bag> create(NewBags newBags, UUID actorUserId, Instant at);

    /** Üretilecek çuvallar. */
    record NewBags(LockedLot lot, PackageType packageType, int count) {}
}
