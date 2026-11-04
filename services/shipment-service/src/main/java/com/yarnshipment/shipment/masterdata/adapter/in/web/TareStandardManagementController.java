package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.shipment.adapter.in.web.api.TareStandardManagementApi;
import com.yarnshipment.shipment.adapter.in.web.api.model.TareStandardListResponse;
import com.yarnshipment.shipment.adapter.in.web.api.model.TareStandardSaveRequest;
import com.yarnshipment.shipment.adapter.in.web.api.model.TareStandardView;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.ListTareStandardsUseCase;
import com.yarnshipment.shipment.masterdata.application.port.in.catalog.SaveTareStandardUseCase;
import com.yarnshipment.shipment.masterdata.domain.catalog.PackagingType;
import com.yarnshipment.shipment.masterdata.domain.catalog.TareStandardDetails;
import com.yarnshipment.shipment.masterdata.domain.model.InvalidMasterDataException;
import java.math.BigDecimal;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Standart dara uçları: okuma MASTER_DATA_VIEW, tanımlama MASTER_DATA_MANAGE (SEC-02: final değildir). */
@RestController
public class TareStandardManagementController implements TareStandardManagementApi {

    private final ListTareStandardsUseCase list;
    private final SaveTareStandardUseCase save;
    private final CurrentActorProvider currentActorProvider;

    public TareStandardManagementController(
            ListTareStandardsUseCase list, SaveTareStandardUseCase save, CurrentActorProvider currentActorProvider) {
        this.list = list;
        this.save = save;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_VIEW')")
    public ResponseEntity<TareStandardListResponse> listTareStandards(@Nullable UUID siteId) {
        return ResponseEntity.ok(new TareStandardListResponse(
                list.listTareStandards(siteId).stream().map(CatalogPages::view).toList()));
    }

    @Override
    @PreAuthorize("hasAuthority('MASTER_DATA_MANAGE')")
    public ResponseEntity<TareStandardView> saveTareStandard(TareStandardSaveRequest request) {
        var saved = save.saveTareStandard(
                currentActorProvider.getCurrentActor(),
                request.getSiteId(),
                PackagingType.valueOf(request.getPackagingType().name()),
                new TareStandardDetails(
                        decimal("standardTareKg", request.getStandardTareKg()),
                        decimal("maxDeviationPct", request.getMaxDeviationPct())));
        return ResponseEntity.ok(CatalogPages.view(saved));
    }

    private static BigDecimal decimal(String field, String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new InvalidMasterDataException(field, "not a number");
        }
    }
}
