package com.yarnshipment.shipment.masterdata.domain.catalog;

import java.util.Objects;
import java.util.UUID;

/**
 * İplik / ürün.
 *
 * @param id kimlik
 * @param code değişmez kod; büyük harf
 * @param details ad ve iplik numarası
 * @param active listelerde görünür mü
 */
public record Product(UUID id, String code, ProductDetails details, boolean active) {

    public Product {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(details, "details");
    }
}
