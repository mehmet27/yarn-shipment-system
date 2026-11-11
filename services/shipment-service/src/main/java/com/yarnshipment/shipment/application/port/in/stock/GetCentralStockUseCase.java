package com.yarnshipment.shipment.application.port.in.stock;

import com.yarnshipment.common.security.Actor;

/** Merkez iplik stokunu ve son hareketleri okur (CENTRAL_STOCK_VIEW). */
public interface GetCentralStockUseCase {

    CentralStockOverview getCentralStock(Actor actor);
}
