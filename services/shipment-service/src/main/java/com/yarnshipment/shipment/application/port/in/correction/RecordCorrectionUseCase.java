package com.yarnshipment.shipment.application.port.in.correction;

/** Kilit sonrası da çalışan gerekçeli düzeltme (rapor 7.8). */
public interface RecordCorrectionUseCase {

    /** Aynı tekrar anahtarıyla gelen ikinci istek düzeltmeyi yeniden uygulamaz, ilk kaydı döner. */
    ShipmentCorrection recordCorrection(RecordCorrectionCommand command);
}
