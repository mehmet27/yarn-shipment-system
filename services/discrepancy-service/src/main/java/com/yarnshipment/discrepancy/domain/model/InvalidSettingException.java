package com.yarnshipment.discrepancy.domain.model;

/** Ayar ekranından gelen değer kural sınırlarının dışında. */
public final class InvalidSettingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String field;

    public InvalidSettingException(String field) {
        super("Invalid setting field=" + field);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
