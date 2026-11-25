package com.yarnshipment.shipment.domain.fabric;

import java.io.Serial;
import java.util.List;

/**
 * Fasoncunun elindeki iplik kumaşa yetmiyor (409, {@code FABRIC_YARN_SHORT}); hangi iplikten kaç kg eksik olduğu
 * iletilir.
 */
public final class FabricYarnShortageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<YarnShortage> shortages;

    public FabricYarnShortageException(List<YarnShortage> shortages) {
        super("Subcontractor lacks yarn for " + shortages.size() + " yarn(s)");
        this.shortages = List.copyOf(shortages);
    }

    public List<YarnShortage> shortages() {
        return shortages;
    }
}
