package com.yarnshipment.evidence.domain.model;

/** Bu yükleme türü ya da dosya biçimi kabul edilmiyor. */
public final class UploadNotAllowedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UploadNotAllowedException(String reason) {
        super("Upload not allowed reason=" + reason);
    }
}
