package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.out.ledger.YarnReturnPort;
import com.yarnshipment.shipment.domain.fabric.YarnReturn;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** İplik dönüşü portunun sahtesi: yazılanları tutar. */
final class RecordingYarnReturnPort implements YarnReturnPort {

    private final List<YarnReturn> saved = new ArrayList<>();

    List<YarnReturn> saved() {
        return saved;
    }

    @Override
    public List<YarnReturn> findByShipment(UUID shipmentId) {
        return saved;
    }

    @Override
    public void save(YarnReturn yarnReturn) {
        saved.add(yarnReturn);
    }
}
