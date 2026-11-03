package com.yarnshipment.discrepancy.application.service;

import com.yarnshipment.common.security.Permission;
import com.yarnshipment.common.security.scope.ScopeGuard;
import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancyQuery;
import com.yarnshipment.discrepancy.application.port.in.ticket.GetDiscrepancyTicketsUseCase;
import com.yarnshipment.discrepancy.application.port.out.ComparisonPort;
import com.yarnshipment.discrepancy.application.port.out.DiscrepancyQueryPort;
import com.yarnshipment.discrepancy.application.port.out.TicketReviewPort;
import com.yarnshipment.discrepancy.domain.model.Discrepancy;
import com.yarnshipment.discrepancy.domain.ticket.ReviewedTicket;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Uyuşmazlık ekranının fiş paneli. Kör giriş gereği uyuşmazlığı yalnızca görebilen kişi (fasoncu) fişleri teslim
 * karşılaştırması yapılmadan göremez; teslimden önce açılan "doğrulanamadı" uyuşmazlığında liste boş döner.
 */
@Service
public class GetDiscrepancyTicketsService implements GetDiscrepancyTicketsUseCase {

    private static final String ACTION = "DISCREPANCY_VIEW";

    private final DiscrepancyAccess access;
    private final ComparisonPort comparisons;
    private final TicketReviews reviews;

    public GetDiscrepancyTicketsService(
            DiscrepancyQueryPort queryPort,
            ComparisonPort comparisons,
            TicketReviewPort reviewPort,
            ScopeGuard scopeGuard) {
        this.access = new DiscrepancyAccess(queryPort, scopeGuard);
        this.comparisons = comparisons;
        this.reviews = new TicketReviews(reviewPort);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewedTicket> getTickets(GetDiscrepancyQuery query) {
        Discrepancy discrepancy = access.load(query.actor(), query.discrepancyId(), ACTION);
        boolean reviewer = query.actor().hasPermission(Permission.DISCREPANCY_RESOLVE);
        if (!reviewer && !comparisons.hasComparison(discrepancy.shipmentId())) {
            return List.of();
        }
        return reviews.of(discrepancy.shipmentId());
    }
}
