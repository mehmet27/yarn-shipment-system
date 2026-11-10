package com.yarnshipment.reporting.application.port.out;

import com.yarnshipment.reporting.domain.model.Dashboard;
import com.yarnshipment.reporting.domain.model.PartyStats;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import com.yarnshipment.reporting.domain.model.ShipmentRow;
import com.yarnshipment.reporting.domain.model.SubcontractorBalance;
import java.time.Instant;
import java.util.List;

/** Okuma modelinden rapor okur. */
public interface ReportQueryPort {

    /** {@code now}, vadesi geçmiş uyuşmazlıkları ayırmak içindir. */
    Dashboard findDashboard(ReportPeriod period, ReportScope scope, Instant now);

    List<PartyStats> findSubcontractorStats(ReportPeriod period, ReportScope scope);

    /** En çok {@code limit} satır; en yeni sevkiyat önce gelir. */
    List<ShipmentRow> findShipments(ReportPeriod period, ReportScope scope, int limit);

    List<SubcontractorBalance> findSubcontractorBalances(ReportPeriod period, ReportScope scope);
}
