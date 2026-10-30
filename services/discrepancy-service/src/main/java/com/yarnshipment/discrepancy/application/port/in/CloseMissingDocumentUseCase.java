package com.yarnshipment.discrepancy.application.port.in;

/** İmzalı teslim belgesi sonradan yüklenince sevkiyatın açık "belge yok" uyuşmazlığını kapatır. */
public interface CloseMissingDocumentUseCase {

    /** Kapatılan uyuşmazlık sayısını döner; açık "belge yok" uyuşmazlığı yoksa hiçbir şey yazmaz. */
    int closeMissingDocument(CloseMissingDocumentCommand command);
}
