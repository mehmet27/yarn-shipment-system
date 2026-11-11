package com.yarnshipment.shipment.application.port.in.fire;

/** Hatalı kg sıfır olan fire dosyasını kapatır (INVOICE_MARK). Kapanmış dosyaya tekrar istek zararsızdır. */
public interface CloseFireSettlementUseCase {

    FireSettlementDetails closeFireSettlement(FireSettlementQuery query);
}
