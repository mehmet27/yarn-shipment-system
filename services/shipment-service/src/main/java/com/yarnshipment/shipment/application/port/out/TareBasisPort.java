package com.yarnshipment.shipment.application.port.out;

import com.yarnshipment.shipment.domain.model.TareBasis;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Çıkış tartımında karşılaştırılacak standart dara (çuval ambalajı). */
public interface TareBasisPort {

    /** Lokasyonun çuval standardından beklenen dara; tanım yoksa ya da standart sıfırsa boş. */
    @Nullable TareBasis findBagTareBasis(UUID siteId, int bagCount);
}
