package com.yarnshipment.shipment.masterdata.application.service;

import com.yarnshipment.shipment.masterdata.application.port.out.LoginEventPort;
import com.yarnshipment.shipment.masterdata.domain.model.AuthUser;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Yayınlanan giriş olaylarını "tür:giriş adı" biçiminde, sırayla tutar. */
final class RecordingLoginEvents implements LoginEventPort {

    private final List<String> events = new ArrayList<>();

    @Override
    public void publishSucceeded(AuthUser user, Instant now) {
        events.add("succeeded:" + user.username());
    }

    @Override
    public void publishFailed(AuthUser user, Instant now) {
        events.add("failed:" + user.username());
    }

    @Override
    public void publishLocked(AuthUser user, Instant lockedUntil, Instant now) {
        events.add("locked:" + user.username() + ":" + lockedUntil);
    }

    List<String> events() {
        return List.copyOf(events);
    }
}
