package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.DriverPageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.DriverView;
import com.yarnshipment.shipment.adapter.in.web.api.model.PageMetadata;
import com.yarnshipment.shipment.adapter.in.web.api.model.SubcontractorPageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.SubcontractorView;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.domain.model.Driver;
import com.yarnshipment.shipment.masterdata.domain.model.Subcontractor;

/** Ana veri sayfalarının web modeli. */
final class MasterDataPages {

    private MasterDataPages() {}

    static SubcontractorPageResponse subcontractors(MasterDataPage<Subcontractor> page) {
        return new SubcontractorPageResponse(
                page.items().stream().map(MasterDataPages::row).toList(), metadata(page));
    }

    static DriverPageResponse drivers(MasterDataPage<Driver> page) {
        return new DriverPageResponse(
                page.items().stream().map(MasterDataPages::row).toList(), metadata(page));
    }

    static SubcontractorView view(Subcontractor item) {
        return new SubcontractorView(item.id(), item.title());
    }

    static DriverView view(Driver item) {
        return new DriverView(item.id(), item.fullName()).subcontractorId(item.subcontractorId());
    }

    private static SubcontractorView row(Subcontractor item) {
        return view(item).active(item.active());
    }

    private static DriverView row(Driver item) {
        return view(item).active(item.active()).subcontractorTitle(item.subcontractorTitle());
    }

    static PageMetadata metadata(MasterDataPage<?> page) {
        return new PageMetadata(page.number(), page.size(), page.totalElements(), page.totalPages());
    }
}
