package com.yarnshipment.reporting.application.port.in;

import com.yarnshipment.reporting.domain.model.ShipmentRow;
import java.util.List;

/** Dönemin sevkiyat dökümünü listeler; en yeni önce gelir. */
public interface ListShipmentReportUseCase {

    List<ShipmentRow> listShipmentReport(ReportQuery query);
}
