package com.yarnshipment.shipment.masterdata.application.port.out;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Hesap açılacak kaydın ve verilecek lokasyonların etkin olup olmadığını okur. */
public interface LoginAccountTargetPort {

    /** Etkin şoförün adı soyadı. */
    Optional<String> findActiveDriverName(UUID id);

    /** Etkin fasoncunun unvanı. */
    Optional<String> findActiveSubcontractorTitle(UUID id);

    /** Verilenler arasından etkin olan lokasyonların kimlikleri. */
    Set<UUID> findActiveSiteIds(Collection<UUID> ids);
}
