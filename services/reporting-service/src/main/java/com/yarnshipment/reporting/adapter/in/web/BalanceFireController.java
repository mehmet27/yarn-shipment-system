package com.yarnshipment.reporting.adapter.in.web;

import com.yarnshipment.common.security.CurrentActorProvider;
import com.yarnshipment.reporting.adapter.in.web.api.BalanceFireApi;
import com.yarnshipment.reporting.adapter.in.web.api.model.BalanceMatrixRowView;
import com.yarnshipment.reporting.adapter.in.web.api.model.FireRowView;
import com.yarnshipment.reporting.adapter.in.web.api.model.OpeningImportRequest;
import com.yarnshipment.reporting.adapter.in.web.api.model.OpeningImportResult;
import com.yarnshipment.reporting.adapter.in.web.api.model.YarnVsFabricRowView;
import com.yarnshipment.reporting.application.port.in.ImportOpeningBalanceUseCase;
import com.yarnshipment.reporting.application.port.in.ListBalanceMatrixUseCase;
import com.yarnshipment.reporting.application.port.in.ListFireReportUseCase;
import com.yarnshipment.reporting.application.port.in.ListToInvoiceUseCase;
import com.yarnshipment.reporting.application.port.in.ListYarnVsFabricUseCase;
import com.yarnshipment.reporting.application.port.in.ReportQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/** Bakiye matrisi, fire raporları ve açılış bakiyesi uçları. Method security proxy'si için final değildir (SEC-02). */
@RestController
public class BalanceFireController implements BalanceFireApi {

    private final ListBalanceMatrixUseCase listBalanceMatrix;
    private final ListFireReportUseCase listFire;
    private final ListYarnVsFabricUseCase listYarnVsFabric;
    private final ListToInvoiceUseCase listToInvoice;
    private final ImportOpeningBalanceUseCase importOpening;
    private final CurrentActorProvider currentActorProvider;

    public BalanceFireController(
            ListBalanceMatrixUseCase listBalanceMatrix,
            ListFireReportUseCase listFire,
            ListYarnVsFabricUseCase listYarnVsFabric,
            ListToInvoiceUseCase listToInvoice,
            ImportOpeningBalanceUseCase importOpening,
            CurrentActorProvider currentActorProvider) {
        this.listBalanceMatrix = listBalanceMatrix;
        this.listFire = listFire;
        this.listYarnVsFabric = listYarnVsFabric;
        this.listToInvoice = listToInvoice;
        this.importOpening = importOpening;
        this.currentActorProvider = currentActorProvider;
    }

    @Override
    @PreAuthorize("hasAuthority('BALANCE_VIEW')")
    public ResponseEntity<List<BalanceMatrixRowView>> getBalanceMatrix(@Nullable UUID siteId) {
        return ResponseEntity.ok(listBalanceMatrix.listBalanceMatrix(query(null, null, siteId)).stream()
                .map(BalanceFireViews::toView)
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<FireRowView>> getFireReport(
            @Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId, @Nullable UUID subcontractorId) {
        return ResponseEntity.ok(listFire.listFire(query(from, to, siteId), subcontractorId).stream()
                .map(BalanceFireViews::toView)
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<YarnVsFabricRowView>> getYarnVsFabricReport(
            @Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return ResponseEntity.ok(listYarnVsFabric.listYarnVsFabric(query(from, to, siteId)).stream()
                .map(BalanceFireViews::toView)
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<List<FireRowView>> getToInvoiceReport(
            @Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return ResponseEntity.ok(listToInvoice.listToInvoice(query(from, to, siteId)).stream()
                .map(BalanceFireViews::toView)
                .toList());
    }

    @Override
    @PreAuthorize("hasAuthority('BALANCE_VIEW')")
    public ResponseEntity<OpeningImportResult> importOpeningBalance(OpeningImportRequest request) {
        var result = importOpening.importOpening(
                currentActorProvider.getCurrentActor().userId(),
                request.getAsOf(),
                request.getRows().stream().map(BalanceFireViews::toDomain).toList());
        return ResponseEntity.ok(new OpeningImportResult(result.imported(), result.skipped()));
    }

    private ReportQuery query(@Nullable LocalDate from, @Nullable LocalDate to, @Nullable UUID siteId) {
        return new ReportQuery(currentActorProvider.getCurrentActor(), from, to, siteId);
    }
}
