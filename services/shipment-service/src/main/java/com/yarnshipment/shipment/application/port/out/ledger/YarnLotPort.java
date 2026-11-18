package com.yarnshipment.shipment.application.port.out.ledger;

import com.yarnshipment.shipment.domain.stock.LotRequest;
import com.yarnshipment.shipment.domain.stock.ResolvedLot;

/** Merkeze iplik girişinde yazılan parti numarasını Partiler'deki partiye bağlar. */
public interface YarnLotPort {

    /**
     * Kullanıcının lokasyonlarında aynı numaralı parti varsa onu verir; yoksa verilen kalite sınıfıyla yeni, kilitsiz
     * parti açar (kalite sonra kilitlenir). Parti başka bir iplik ya da tedarikçiye aitse, kullanıcının lokasyonu yoksa
     * ya da yeni parti için kalite sınıfı verilmemişse {@code YarnLotConflictException}.
     */
    ResolvedLot resolve(LotRequest request);
}
