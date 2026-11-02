package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.LotPageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.LotView;
import com.yarnshipment.shipment.adapter.in.web.api.model.ProductPageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.ProductView;
import com.yarnshipment.shipment.adapter.in.web.api.model.QualityGradePageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.QualityGradeView;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataPage;
import com.yarnshipment.shipment.masterdata.domain.catalog.Product;
import com.yarnshipment.shipment.masterdata.domain.catalog.QualityGrade;
import com.yarnshipment.shipment.masterdata.domain.lot.ProductionLot;
import java.time.Instant;
import java.time.ZoneOffset;

/** İplik, kalite sınıfı ve parti sayfalarının web modeli. */
final class CatalogPages {

    private CatalogPages() {}

    static ProductView view(Product item) {
        return new ProductView(item.id(), item.code(), item.details().name())
                .yarnCount(item.details().yarnCount())
                .active(item.active());
    }

    static QualityGradeView view(QualityGrade item) {
        return new QualityGradeView(item.id(), item.code(), item.details().name())
                .labelColor(item.details().labelColor())
                .active(item.active());
    }

    static LotView view(ProductionLot item) {
        Instant lockedAt = item.qualityLockedAt();
        return new LotView(
                        item.id(),
                        item.siteId(),
                        item.labels().siteName(),
                        item.details().lotNo(),
                        item.details().productId(),
                        item.labels().productCode(),
                        item.labels().productName(),
                        item.details().qualityGradeId(),
                        item.labels().qualityGradeCode(),
                        item.labels().qualityGradeName(),
                        item.details().productionDate(),
                        item.active())
                .qualityLockedAt(lockedAt == null ? null : lockedAt.atOffset(ZoneOffset.UTC));
    }

    static ProductPageResponse products(MasterDataPage<Product> page) {
        return new ProductPageResponse(
                page.items().stream().map(CatalogPages::view).toList(), MasterDataPages.metadata(page));
    }

    static QualityGradePageResponse qualityGrades(MasterDataPage<QualityGrade> page) {
        return new QualityGradePageResponse(
                page.items().stream().map(CatalogPages::view).toList(), MasterDataPages.metadata(page));
    }

    static LotPageResponse lots(MasterDataPage<ProductionLot> page) {
        return new LotPageResponse(
                page.items().stream().map(CatalogPages::view).toList(), MasterDataPages.metadata(page));
    }
}
