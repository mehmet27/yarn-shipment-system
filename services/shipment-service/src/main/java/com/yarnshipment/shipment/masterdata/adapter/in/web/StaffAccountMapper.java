package com.yarnshipment.shipment.masterdata.adapter.in.web;

import com.yarnshipment.shipment.adapter.in.web.api.model.StaffLoginAccountRequest;
import com.yarnshipment.shipment.masterdata.application.port.in.account.StaffAccountRequest;
import com.yarnshipment.shipment.masterdata.domain.model.StaffRole;
import com.yarnshipment.shipment.masterdata.domain.model.StaffWorkArea;

/** Hesap açma gövdesini uygulama isteğine çevirir. */
final class StaffAccountMapper {

    private StaffAccountMapper() {}

    static StaffAccountRequest toRequest(StaffLoginAccountRequest request) {
        return new StaffAccountRequest(
                request.getUsername(),
                request.getFullName(),
                StaffRole.valueOf(request.getRole().name()),
                request.getPassword(),
                request.getWorkArea() == null
                        ? null
                        : StaffWorkArea.valueOf(request.getWorkArea().name()));
    }
}
