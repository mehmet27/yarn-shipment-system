package com.yarnshipment.shipment.masterdata.application.port.out;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/** Hesap açılacak kaydın ve verilecek lokasyonların etkin olup olmadığını okur. */
public interface LoginAccountTargetPort {

    /** Verilenler arasından etkin olan lokasyonların kimlikleri. */
    Set<UUID> findActiveSiteIds(Collection<UUID> ids);

    /** Tüm etkin lokasyonların kimlikleri; fabrikada tek merkez lokasyon olduğu için yeni hesaplar hepsini görür. */
    Set<UUID> findAllActiveSiteIds();
}
