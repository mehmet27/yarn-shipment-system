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

    static FireRowView toView(FireFile f) {
        var view = new FireRowView(
                f.shipmentId(),
                f.shipmentNo(),
                f.subcontractorId(),
                f.subcontractorName(),
                f.sentKg().toPlainString(),
                f.returnedYarnKg().toPlainString(),
                f.fabricKg().toPlainString(),
                f.fireRate().toPlainString(),
                f.expectedFabricKg().toPlainString(),
                f.actualFireRate().toPlainString(),
                f.shortageKg().toPlainString(),
                FireRowView.StatusEnum.fromValue(f.status()));
        view.setInvoiceNo(f.invoiceNo());
        if (f.invoicedAt() != null) {
            view.setInvoicedAt(f.invoicedAt().atOffset(ZoneOffset.UTC));
        }
        return view;
    }

    static YarnVsFabricRowView toView(YarnVsFabricRow r) {
        return new YarnVsFabricRowView(
                r.subcontractorId(),
                r.subcontractorName(),
                r.sentKg().toPlainString(),
                r.returnedYarnKg().toPlainString(),
                r.fabricKg().toPlainString(),
                r.actualFireRate().toPlainString(),
                r.shortageKg().toPlainString());
    }

    static OpeningBalanceRow toDomain(OpeningRowRequest row) {
        return new OpeningBalanceRow(
                row.getSubcontractorId(),
                row.getSubcontractorName(),
                row.getYarnType(),
                new BigDecimal(row.getNetKg()).setScale(FireFile.KG_SCALE));
    }
}
