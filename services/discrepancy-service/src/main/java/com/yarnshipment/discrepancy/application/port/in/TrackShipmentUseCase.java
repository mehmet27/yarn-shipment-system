package com.yarnshipment.discrepancy.application.port.in;

/** İzlenen sevkiyatın yola çıkış, varış ve kapanış zamanlarını yazar. */
public interface TrackShipmentUseCase {

    void trackShipment(TrackShipmentCommand command);
}
