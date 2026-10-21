package com.yarnshipment.shipment.application.service;

import com.yarnshipment.shipment.application.port.out.DispatchCommandPort;
import com.yarnshipment.shipment.domain.model.DispatchRecording;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Kaydedilen çıkış kayıtlarını bellekte tutar. */
final class RecordingDispatchCommandPort implements DispatchCommandPort {

    private final List<DispatchRecording> recordings = new ArrayList<>();

    List<DispatchRecording> recordings() {
        return recordings;
    }

    void addExisting(DispatchRecording recording) {
        recordings.add(recording);
    }

    @Override
    public boolean hasDispatchWeighing(UUID shipmentId) {
        return recordings.stream().anyMatch(recording -> recording.shipmentId().equals(shipmentId));
    }

    @Override
    public void recordDispatch(DispatchRecording recording, UUID actorUserId) {
        recordings.add(recording);
    }
}
