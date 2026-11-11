package com.yarnshipment.shipment.adapter.in.web.ledger;

/** Sayfa bilgisi; mevcut liste uçlarıyla aynı biçim (API-06). */
record PageMetadataView(int number, int size, long totalElements, int totalPages) {}
