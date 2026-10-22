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

    static VehicleView view(Vehicle item) {
        return new VehicleView(item.id(), item.plateNo());
    }

    static SiteView view(Site item) {
        return new SiteView(item.id(), item.name());
    }

    private static VehicleView row(Vehicle item) {
        return view(item).active(item.active());
    }

    private static SiteView row(Site item) {
        return view(item).active(item.active());
    }

    static VehiclePageResponse vehicles(MasterDataPage<Vehicle> page) {
        return new VehiclePageResponse(
                page.items().stream().map(VehicleSitePages::row).toList(), MasterDataPages.metadata(page));
    }

    static SitePageResponse sites(MasterDataPage<Site> page) {
        return new SitePageResponse(
                page.items().stream().map(VehicleSitePages::row).toList(), MasterDataPages.metadata(page));
    }
}
