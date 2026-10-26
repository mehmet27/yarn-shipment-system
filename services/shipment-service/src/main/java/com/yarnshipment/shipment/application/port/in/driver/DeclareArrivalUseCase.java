package com.yarnshipment.shipment.application.port.in.driver;

/** Şoför fasoncuya varır; sevkiyat teslim beklemeye geçer. */
public interface DeclareArrivalUseCase {

    void declareArrival(DeclareArrivalCommand command);
}
