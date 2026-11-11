package com.yarnshipment.shipment.application.port.in;

/** Çuvalları sıra no ve kiloyla taslak sevkiyata ekler. */
public interface AssignBagsUseCase {

    /** Eklenen çuval sayısı. */
    int assignBags(AssignBagsCommand command);
}
