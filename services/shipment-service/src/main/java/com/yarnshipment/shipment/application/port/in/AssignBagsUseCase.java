package com.yarnshipment.shipment.application.port.in;

/** Çuvalları taslak sevkiyata atar; çuval servisinde ayırır ve sevkiyatın çuval listesine yazar. */
public interface AssignBagsUseCase {

    /** Bu istekle yeni atanan çuval sayısı; daha önce atanmış çuvallar sayılmaz. */
    int assignBags(AssignBagsCommand command);
}
