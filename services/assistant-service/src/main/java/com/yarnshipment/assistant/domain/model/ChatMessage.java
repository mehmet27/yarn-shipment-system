package com.yarnshipment.assistant.domain.model;

import java.util.Objects;

/**
 * Sohbetteki tek mesaj.
 *
 * @param speaker konuşan taraf
 * @param text mesaj metni
 */
public record ChatMessage(Speaker speaker, String text) {

    public ChatMessage {
        Objects.requireNonNull(speaker, "speaker");
        Objects.requireNonNull(text, "text");
    }
}
