package com.yarnshipment.discrepancy.application.port.in;

/** Kullanıcının kapsamındaki uyuşmazlıkları listeler. */
public interface ListDiscrepanciesUseCase {

    DiscrepancyPage listDiscrepancies(ListDiscrepanciesQuery query);
}
