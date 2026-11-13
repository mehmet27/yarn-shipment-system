package com.yarnshipment.events.stock;

/** Merkez stok işlemlerinin yayınlandığı konu (EVT-04); denetim kaydında varlık türü {@code CentralStock} olur. */
public final class StockEvents {

    public static final String TOPIC = "stock.events.v1";

    private StockEvents() {}
}
