package com.yarnshipment.evidence.domain.model;

/** İmzalı dosya adresi kullanılamadı: adres geçersiz ya da süresi dolmuş, dosya yok ya da dosya imzayla uyuşmuyor. */
public final class SignedFileException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Hatanın nedeni. */
    public enum Reason {
        INVALID_LINK,
        EXPIRED_LINK,
        FILE_NOT_FOUND,
        SIZE_MISMATCH,
        TYPE_MISMATCH
    }

    private final Reason reason;

    public SignedFileException(Reason reason) {
        super("Signed file request refused reason=" + reason);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
