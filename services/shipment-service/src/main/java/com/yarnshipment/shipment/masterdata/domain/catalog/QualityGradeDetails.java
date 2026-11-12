package com.yarnshipment.shipment.masterdata.domain.catalog;

import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import com.yarnshipment.shipment.masterdata.domain.model.MasterDataText;

/**
 * Kalite sınıfının düzenlenebilir alanları. Kod yalnızca eklerken verilir.
 *
 * @param name ad, ör. {@code Birinci kalite}
 * @param sortOrder listelerde görünme sırası, 0-9999; küçük olan önce gelir
 */
public record QualityGradeDetails(String name, int sortOrder) {

    public static final int MAX_SORT_ORDER = 9999;

    public QualityGradeDetails {
        name = MasterDataText.clean("name", name);
        if (sortOrder < 0 || sortOrder > MAX_SORT_ORDER) {
            throw new InvalidMasterDataException("sortOrder", "out of range");
        }
    }
}
