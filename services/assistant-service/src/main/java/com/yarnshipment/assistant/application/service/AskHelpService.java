package com.yarnshipment.assistant.application.service;

import com.yarnshipment.assistant.application.port.in.AskHelpCommand;
import com.yarnshipment.assistant.application.port.in.AskHelpUseCase;
import com.yarnshipment.assistant.application.port.in.GetHelpStatusUseCase;
import com.yarnshipment.assistant.application.port.out.HelpKnowledgePort;
import com.yarnshipment.assistant.application.port.out.HelpModelPort;
import com.yarnshipment.assistant.domain.model.ChatMessage;
import com.yarnshipment.assistant.domain.model.HelpRateLimitedException;
import com.yarnshipment.assistant.domain.model.HelpUnavailableException;
import com.yarnshipment.assistant.domain.model.Speaker;
import com.yarnshipment.common.security.Actor;
import com.yarnshipment.common.security.Role;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/** Soruyu, kullanıcının rolü ve proje dokümanlarıyla birlikte modele iletir. Hiçbir veriyi değiştirmez. */
@Service
final class AskHelpService implements AskHelpUseCase, GetHelpStatusUseCase {

    /** Modele giden önceki mesaj sayısı; sohbet uzadıkça maliyet artmasın. */
    static final int MAX_HISTORY_MESSAGES = 10;

    static final int MAX_HISTORY_MESSAGE_LENGTH = 2000;

    private static final ZoneId ISTANBUL = ZoneId.of("Europe/Istanbul");

    private final HelpKnowledgePort knowledge;
    private final HelpModelPort model;
    private final QuestionRateLimiter rateLimiter;
    private final Clock clock;

    AskHelpService(HelpKnowledgePort knowledge, HelpModelPort model, QuestionRateLimiter rateLimiter, Clock clock) {
        this.knowledge = knowledge;
        this.model = model;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    @Override
    public boolean isAvailable() {
        return model.isConfigured();
    }

    @Override
    public String ask(AskHelpCommand command) {
        if (!model.isConfigured()) {
            throw new HelpUnavailableException("help assistant is not configured");
        }
        if (command.actor().roles().isEmpty()) {
            // Geçici şifresini değiştirmemiş kişinin rolü ve izni yoktur; belgeler ve API bütçesi ona açılmaz.
            throw new HelpUnavailableException("password change required");
        }
        if (!rateLimiter.tryAcquire(command.actor().userId())) {
            throw new HelpRateLimitedException("question limit reached");
        }
        return model.complete(
                knowledge.documents(),
                UserContext.describe(command.actor(), LocalDate.now(clock.withZone(ISTANBUL))),
                conversation(command));
    }

    private static List<ChatMessage> conversation(AskHelpCommand command) {
        List<ChatMessage> history = command.history();
        int from = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        List<ChatMessage> messages = new ArrayList<>();
        // Konuşma kullanıcıyla başlamalı ve taraflar sırayla gelmeli; bozuk geçmiş yok sayılır.
        Speaker expected = Speaker.USER;
        for (ChatMessage message : history.subList(from, history.size())) {
            if (message.speaker() == expected && !message.text().isBlank()) {
                messages.add(new ChatMessage(message.speaker(), shorten(message.text())));
                expected = expected == Speaker.USER ? Speaker.ASSISTANT : Speaker.USER;
            }
        }
        if (expected == Speaker.ASSISTANT) {
            // Son geçmiş mesajı kullanıcınınsa cevapsız kalmıştır; yeni soru ayrı bir tur olabilsin diye atılır.
            messages.removeLast();
        }
        messages.add(new ChatMessage(Speaker.USER, command.question()));
        return List.copyOf(messages);
    }

    private static String shorten(String text) {
        return text.length() <= MAX_HISTORY_MESSAGE_LENGTH ? text : text.substring(0, MAX_HISTORY_MESSAGE_LENGTH);
    }

    /** Soran kişinin rollerini günlük dilde anlatır; cevap buna göre verilir. */
    private static final class UserContext {

        private UserContext() {}

        static String describe(Actor actor, LocalDate today) {
            StringBuilder text = new StringBuilder("Şu an soran kullanıcının rolleri: ");
            text.append(String.join(
                    ", ",
                    actor.roles().stream().map(UserContext::label).sorted().toList()));
            text.append(".\nBugünün tarihi: ").append(today).append(" (Europe/Istanbul).");
            text.append("\nCevabı bu role göre ver: rolün yapamadığı bir işi sorarsa bunu yapabilecek rolü söyle.");
            return text.toString();
        }

        private static String label(Role role) {
            return switch (role) {
                case ADMIN -> "Yönetici";
                case WAREHOUSE -> "Depo";
                case QUALITY -> "Kalite";
                case MANAGER -> "Müdür";
                case GENERAL_MANAGER -> "Genel müdür";
                case SYSTEM -> "Sistem";
            };
        }
    }
}
