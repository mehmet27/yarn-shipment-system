package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.SitePageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.SiteView;
import com.yarnshipment.shipment.adapter.in.web.api.model.VehiclePageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.VehicleView;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.domain.model.Site;
import com.yarnshipment.shipment.masterdata.domain.model.Vehicle;

/** Araç ve lokasyon sayfalarının web modeli. */
final class VehicleSitePages {

    private VehicleSitePages() {}

    static VehiclePageResponse vehicles(MasterDataPage<Vehicle> page) {
        return new VehiclePageResponse(
                page.items().stream()
                        .map(item -> new VehicleView(item.id(), item.plateNo()))
                        .toList(),
                MasterDataPages.metadata(page));
    }

    static SitePageResponse sites(MasterDataPage<Site> page) {
        return new SitePageResponse(
                page.items().stream()
                        .map(item -> new SiteView(item.id(), item.name()))
                        .toList(),
                MasterDataPages.metadata(page));
    }
}
