package com.yarnshipment.bag.application.port.in;

/**
 * Kalitesi kilitlenen partiyi yerel kopyaya yazar; çuval yalnızca bu kopyada kaydı olan partiye bağlanabilir ve
 * kalitesini buradan alır (rapor Bölüm 10). Aynı olayın tekrarı etkisizdir.
 */
public interface RecordLotQualityLockUseCase {

    void recordLotQualityLock(RecordLotQualityLockCommand command);
}
