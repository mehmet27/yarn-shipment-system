package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.YarnSupplierPageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.YarnSupplierView;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.domain.catalog.YarnSupplier;

/** İplik tedarikçisi sayfalarının web modeli. */
final class YarnSupplierPages {

    private YarnSupplierPages() {}

    static YarnSupplierView view(YarnSupplier item) {
        return new YarnSupplierView(item.id(), item.name()).active(item.active());
    }

    static YarnSupplierPageResponse yarnSuppliers(MasterDataPage<YarnSupplier> page) {
        return new YarnSupplierPageResponse(
                page.items().stream().map(YarnSupplierPages::view).toList(), MasterDataPages.metadata(page));
    }
}
