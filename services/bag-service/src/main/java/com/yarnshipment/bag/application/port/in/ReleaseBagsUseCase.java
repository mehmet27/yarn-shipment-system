package com.yarnshipment.bag.application.port.in;

/** Sevkiyatın rezervasyonlarını bırakır ve çuvalları yeniden kullanılabilir yapar. */
public interface ReleaseBagsUseCase {

    /** Bırakılan rezervasyon sayısı; bırakılacak yoksa 0. */
    int releaseBags(ReleaseBagsCommand command);
}
