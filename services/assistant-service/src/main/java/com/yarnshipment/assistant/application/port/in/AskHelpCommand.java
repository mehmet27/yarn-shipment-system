package com.yarnshipment.assistant.application.port.in;

import com.yarnshipment.assistant.domain.model.ChatMessage;
import com.yarnshipment.assistant.domain.model.InvalidHelpQuestionException;
import com.yarnshipment.common.security.Actor;
import java.util.List;

/**
 * Yardım sorusu.
 *
 * @param actor soran kullanıcı
 * @param question yeni soru
 * @param history sorudan önceki mesajlar, eskiden yeniye
 */
public record AskHelpCommand(Actor actor, String question, List<ChatMessage> history) {

    /** Bir sorunun en çok uzunluğu (karakter). */
    public static final int MAX_QUESTION_LENGTH = 1000;

    public AskHelpCommand {
        if (question.isBlank()) {
            throw new InvalidHelpQuestionException("question is blank");
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new InvalidHelpQuestionException("question is longer than " + MAX_QUESTION_LENGTH);
        }
        history = List.copyOf(history);
    }
}
