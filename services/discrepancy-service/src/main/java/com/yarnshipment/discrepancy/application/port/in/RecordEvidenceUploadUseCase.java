package com.yarnshipment.discrepancy.application.port.in;

/**
 * Yüklenen kanıt türünü sevkiyata yazar. Kanıt sonradan geldiyse sevkiyatın onun eksikliği için açılmış uyuşmazlığını
 * ("belge yok", "fotoğraf yok") kapatır.
 */
public interface RecordEvidenceUploadUseCase {

    /** Kapatılan uyuşmazlık sayısını döner; kapatılacak uyuşmazlık yoksa yalnızca kanıt türü yazılır. */
    int recordEvidenceUpload(RecordEvidenceUploadCommand command);
}
