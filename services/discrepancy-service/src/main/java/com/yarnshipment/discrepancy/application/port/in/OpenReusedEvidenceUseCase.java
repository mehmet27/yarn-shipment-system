package com.yarnshipment.discrepancy.application.port.in;

/** Tekrar kullanılan fotoğraf için sevkiyatta Kırmızı fotoğraf uyuşmazlığı açar. */
public interface OpenReusedEvidenceUseCase {

    /** Açıldıysa {@code true}; olay tekrar geldiyse ya da sevkiyat izlenmiyorsa {@code false}. */
    boolean openReusedEvidence(OpenReusedEvidenceCommand command);
}
