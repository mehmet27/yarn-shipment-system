package com.yarnshipment.reporting.domain.model;

/** Rapor dönemi geçersiz: başlangıç bitişten sonra ya da dönem çok uzun. */
public final class InvalidReportPeriodException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidReportPeriodException(String message) {
        super(message);
    }
}
