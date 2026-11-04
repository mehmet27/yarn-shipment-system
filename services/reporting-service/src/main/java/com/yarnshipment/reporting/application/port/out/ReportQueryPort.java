package com.yarnshipment.reporting.application.port.out;

import com.yarnshipment.reporting.domain.model.Dashboard;
import com.yarnshipment.reporting.domain.model.PairStats;
import com.yarnshipment.reporting.domain.model.PartyStats;
import com.yarnshipment.reporting.domain.model.ReportPeriod;
import java.time.Instant;
import java.util.List;

/** Okuma modelinden rapor okur. */
public interface ReportQueryPort {

    /** {@code now}, vadesi geçmiş uyuşmazlıkları ayırmak içindir. */
    Dashboard findDashboard(ReportPeriod period, ReportScope scope, Instant now);

    List<PartyStats> findDriverStats(ReportPeriod period, ReportScope scope);

    List<PartyStats> findSubcontractorStats(ReportPeriod period, ReportScope scope);

    List<PairStats> findPairStats(ReportPeriod period, ReportScope scope);
}
