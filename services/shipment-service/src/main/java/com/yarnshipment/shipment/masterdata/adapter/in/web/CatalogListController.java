package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.CatalogListsApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.LotPageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.MasterDataStatusFilter;
import com.yarnshipment.shipment.adapter.in.web.api.model.ProductPageResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.QualityGradePageResponse;
import com.yarnshipment.shipment.masterdata.application.port.in.MasterDataQuery;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ListProductsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ListQualityGradesUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.lot.ListLotsUseCase;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * İplik, kalite sınıfı ve parti okuma uçları. Pasif iplik ve kaliteyi yönetici, pasif partiyi partiyi yöneten kalite
 * kullanıcısı da isteyebilir (SEC-02: final değildir).
 */
@RestController
public class CatalogListController implements CatalogListsApi {

    private static final String LIST_ACCESS =
            "hasAuthority('MASTER_DATA_VIEW') and (#status?.name() != 'ALL' or hasAuthority('MASTER_DATA_MANAGE'))";
    private static final String LOT_LIST_ACCESS =
            "hasAuthority('MASTER_DATA_VIEW') and (#status?.name() != 'ALL' or hasAuthority('LOT_QUALITY_LOCK'))";

    private final ListProductsUseCase products;
    private final ListQualityGradesUseCase qualityGrades;
    private final ListLotsUseCase lots;
    private final CurrentActorProvider currentActorProvider;

    public CatalogListController(
            ListProductsUseCase products,
            ListQualityGradesUseCase qualityGrades,
            ListLotsUseCase lots,
            CurrentActorProvider currentActorProvider) {
        this.products = products;
        this.qualityGrades = qualityGrades;
        this.lots = lots;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize(LIST_ACCESS)
    public ResponseEntity<ProductPageResponse> listProducts(
            Integer page, Integer size, @Nullable String search, MasterDataStatusFilter status) {
        return ResponseEntity.ok(CatalogPages.products(products.listProducts(query(page, size, search, status))));
    }

    @Override
    @PreAuthorize(LIST_ACCESS)
    public ResponseEntity<QualityGradePageResponse> listQualityGrades(
            Integer page, Integer size, @Nullable String search, MasterDataStatusFilter status) {
        return ResponseEntity.ok(
                CatalogPages.qualityGrades(qualityGrades.listQualityGrades(query(page, size, search, status))));
    }

    @Override
    @PreAuthorize(LOT_LIST_ACCESS)
    public ResponseEntity<LotPageResponse> listLots(
            Integer page, Integer size, @Nullable String search, MasterDataStatusFilter status) {
        return ResponseEntity.ok(CatalogPages.lots(
                lots.listLots(currentActorProvider.getCurrentActor(), query(page, size, search, status))));
    }

    private static MasterDataQuery query(
            Integer page, Integer size, @Nullable String search, MasterDataStatusFilter status) {
        return new MasterDataQuery(search, page, size, status == MasterDataStatusFilter.ALL);
    }
}
