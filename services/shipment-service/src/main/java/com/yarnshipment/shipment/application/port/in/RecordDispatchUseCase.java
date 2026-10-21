package com.yarnshipment.shipment.application.port.in;

/** Depo çıkış tartımını ve mührü kaydeder; sevkiyat yüklendi olur. */
public interface RecordDispatchUseCase {

    void recordDispatch(RecordDispatchCommand command);
}
