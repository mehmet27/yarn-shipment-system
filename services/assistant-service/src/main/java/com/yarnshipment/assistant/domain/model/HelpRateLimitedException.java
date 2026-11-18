package com.yarnshipment.assistant.domain.model;

/** Kullanıcı kısa sürede çok soru sordu; maliyeti sınırlamak için bir süre beklemesi gerekir. */
public final class HelpRateLimitedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public HelpRateLimitedException(String message) {
        super(message);
    }
}
