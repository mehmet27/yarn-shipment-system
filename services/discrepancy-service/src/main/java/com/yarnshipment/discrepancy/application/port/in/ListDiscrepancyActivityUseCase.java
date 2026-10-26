package com.yarnshipment.discrepancy.application.port.in;

import com.yarnshipment.discrepancy.domain.model.DiscrepancyActivity;
import java.util.List;

/** Uyuşmazlık geçmişini en eskiden yeniye listeler. */
public interface ListDiscrepancyActivityUseCase {

    List<DiscrepancyActivity> listActivity(GetDiscrepancyQuery query);
}
