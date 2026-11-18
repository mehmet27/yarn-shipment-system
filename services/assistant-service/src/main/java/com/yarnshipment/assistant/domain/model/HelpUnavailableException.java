package com.yarnshipment.assistant.domain.model;

/** Yardım asistanı şu an cevap veremiyor: anahtar tanımlı değil ya da model servisine ulaşılamadı. */
public final class HelpUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public HelpUnavailableException(String message) {
        super(message);
    }

    public HelpUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
