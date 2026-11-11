package com.yarnshipment.reporting.adapter.in.web;

import com.yarnshipment.reporting.adapter.in.web.api.model.BalanceMatrixRowView;
import com.yarnshipment.reporting.adapter.in.web.api.model.FireRowView;
import com.yarnshipment.reporting.adapter.in.web.api.model.OpeningRowRequest;
import com.yarnshipment.reporting.adapter.in.web.api.model.YarnVsFabricRowView;
import com.yarnshipment.reporting.domain.model.BalanceMatrixRow;
import com.yarnshipment.reporting.domain.model.FireFile;
import com.yarnshipment.reporting.domain.model.OpeningBalanceRow;
import com.yarnshipment.reporting.domain.model.YarnVsFabricRow;
import java.math.BigDecimal;
import java.time.ZoneOffset;

/** Bakiye matrisi ve fire satırlarını sözleşme tiplerine çevirir. */
final class BalanceFireViews {

    private BalanceFireViews() {}

    static BalanceMatrixRowView toView(BalanceMatrixRow row) {
        return new BalanceMatrixRowView(
                row.yarnType(),
                row.subcontractorId(),
                row.subcontractorName(),
                row.balanceKg().toPlainString(),
                row.inTransitKg().toPlainString());
    }

    static FireRowView toView(FireFile file) {
        var view = new FireRowView(
                file.shipmentId(),
                file.shipmentNo(),
                file.subcontractorId(),
                file.subcontractorName(),
                file.sentKg().toPlainString(),
                file.returnedYarnKg().toPlainString(),
                file.fabricKg().toPlainString(),
                file.fireRate().toPlainString(),
                file.expectedFabricKg().toPlainString(),
                file.actualFireRate().toPlainString(),
                file.shortageKg().toPlainString(),
                FireRowView.StatusEnum.fromValue(file.status()));
        view.setInvoiceNo(file.invoiceNo());
        if (file.invoicedAt() != null) {
            view.setInvoicedAt(file.invoicedAt().atOffset(ZoneOffset.UTC));
        }
        return view;
    }

    static YarnVsFabricRowView toView(YarnVsFabricRow row) {
        return new YarnVsFabricRowView(
                row.subcontractorId(),
                row.subcontractorName(),
                row.sentKg().toPlainString(),
                row.returnedYarnKg().toPlainString(),
                row.fabricKg().toPlainString(),
                row.actualFireRate().toPlainString(),
                row.shortageKg().toPlainString());
    }

    static OpeningBalanceRow toDomain(OpeningRowRequest row) {
        return new OpeningBalanceRow(
                row.getSubcontractorId(),
                row.getSubcontractorName(),
                row.getYarnType(),
                new BigDecimal(row.getNetKg()).setScale(FireFile.KG_SCALE));
    }
}
