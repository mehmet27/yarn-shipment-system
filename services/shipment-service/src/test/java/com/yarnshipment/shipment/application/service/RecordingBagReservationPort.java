package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.out.BagReservationPort;
import com.yarnshipment.shipment.domain.bag.BagReservationException;
import com.yarnshipment.shipment.domain.bag.BagReservationException.Reason;
import com.yarnshipment.shipment.domain.bag.BagServiceUnavailableException;
import com.yarnshipment.shipment.domain.bag.ReservedBag;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Çuval servisini taklit eder: istenen anahtarları ve bırakılan sevkiyatları kaydeder, istenirse reddeder. */
final class RecordingBagReservationPort implements BagReservationPort {

    private static final ReservedBag BAG = new ReservedBag(UUID.randomUUID(), "BAG-0001", "P-2026-0001", "A");

    private final List<String> reservedKeys = new ArrayList<>();
    private final List<UUID> released = new ArrayList<>();
    private boolean rejectReservation;
    private boolean failOnRelease;

    List<String> reservedKeys() {
        return reservedKeys;
    }

    List<UUID> released() {
        return released;
    }

    void rejectReservation() {
        rejectReservation = true;
    }

    void failOnRelease() {
        failOnRelease = true;
    }

    @Override
    public List<ReservedBag> reserve(UUID shipmentId, List<String> uniqueKeys) {
        if (rejectReservation) {
            throw new BagReservationException(Reason.ALREADY_RESERVED);
        }
        reservedKeys.addAll(uniqueKeys);
        return List.of(BAG);
    }

    @Override
    public void release(UUID shipmentId, String reason) {
        if (failOnRelease) {
            throw new BagServiceUnavailableException("down");
        }
        released.add(shipmentId);
    }
}
