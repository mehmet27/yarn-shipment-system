package com.yarnshipment.assistant.domain.model;

/** Soru boş ya da çok uzun. */
public final class InvalidHelpQuestionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidHelpQuestionException(String message) {
        super(message);
    }
}
