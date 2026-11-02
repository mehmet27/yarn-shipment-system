package com.yarnshipment.shipment.masterdata.domain.catalog;

import com.yarnshipment.shipment.masterdata.domain.model.MasterDataText;
import org.jspecify.annotations.Nullable;

/**
 * İpliğin düzenlenebilir alanları. Kod yalnızca eklerken verilir.
 *
 * @param name ad, ör. {@code Polyester 30/1}
 * @param yarnCount iplik numarası, ör. {@code Ne 30/1}; isteğe bağlı
 */
public record ProductDetails(String name, @Nullable String yarnCount) {

    public ProductDetails {
        name = MasterDataText.clean("name", name);
        yarnCount = OptionalText.clean("yarnCount", yarnCount);
    }
}
