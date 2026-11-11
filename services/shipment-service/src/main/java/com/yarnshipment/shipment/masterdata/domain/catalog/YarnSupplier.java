package com.yarnshipment.shipment.masterdata.domain.catalog;

import com.yarnshipment.shipment.masterdata.domain.model.MasterDataText;
import java.util.Objects;
import java.util.UUID;

/**
 * İplik tedarikçisi (Şireci, Karesi ...). Ad büyük/küçük harf duyarsız tekildir.
 *
 * @param id kimlik
 * @param name ad
 * @param active listelerde görünür mü; pasif tedarikçi yeni partiye verilemez
 */
public record YarnSupplier(UUID id, String name, boolean active) {

    public YarnSupplier {
        Objects.requireNonNull(id, "id");
        name = MasterDataText.clean("name", name);
    }
}
