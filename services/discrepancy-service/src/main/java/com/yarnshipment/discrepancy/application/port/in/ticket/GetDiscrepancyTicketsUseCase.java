package com.yarnshipment.discrepancy.application.port.in.ticket;

import com.yarnshipment.discrepancy.application.port.in.GetDiscrepancyQuery;
import com.yarnshipment.discrepancy.domain.ticket.ReviewedTicket;
import java.util.List;

/** Uyuşmazlığın sevkiyatındaki kantar fişleri: okunan ve girilen rakamlar yan yana. */
public interface GetDiscrepancyTicketsUseCase {

    List<ReviewedTicket> getTickets(GetDiscrepancyQuery query);
}
