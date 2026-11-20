package com.yarnshipment.shipment.domain.stock;

import java.io.Serial;
import java.util.List;

/**
 * Merkezden çıkan sevkiyatın reçetesinde Tanımlar'da olmayan (serbest metinle yazılmış) iplik var (409,
 * {@code YARN_TYPE_NOT_IN_CATALOG}). Tanımsız ipliğin merkez stoğu denetlenemediği için sevkiyat tamamlanmaz; iplik
 * Tanımlar'a aynı adla eklenince tamamlama yinelenir.
 */
public final class UnlistedYarnTypeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<String> yarnTypes;

    public UnlistedYarnTypeException(List<String> yarnTypes) {
        super("Yarn type(s) not in catalog: " + yarnTypes.size());
        this.yarnTypes = List.copyOf(yarnTypes);
    }

    public List<String> yarnTypes() {
        return yarnTypes;
    }
}
