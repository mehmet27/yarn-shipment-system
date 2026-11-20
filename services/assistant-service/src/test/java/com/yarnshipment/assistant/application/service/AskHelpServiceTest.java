package com.yarnshipment.assistant.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.assistant.TestTokens;
import com.yarnshipment.assistant.application.port.in.AskHelpCommand;
import com.yarnshipment.assistant.application.port.out.HelpModelPort;
import com.yarnshipment.assistant.domain.model.ChatMessage;
import com.yarnshipment.assistant.domain.model.HelpRateLimitedException;
import com.yarnshipment.assistant.domain.model.HelpUnavailableException;
import com.yarnshipment.assistant.domain.model.InvalidHelpQuestionException;
import com.yarnshipment.assistant.domain.model.Speaker;
import com.yarnshipment.common.security.Actor;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Asistanın modele ne ilettiği, geçmişin kırpılması ve soru sınırı. */
class AskHelpServiceTest {

    private static final int PAIRS = 8;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneOffset.UTC);

    private final RecordingModel model = new RecordingModel(true);
    private final AskHelpService service =
            new AskHelpService(() -> "BELGELER", model, new QuestionRateLimiter(CLOCK), CLOCK);

    @Test
    @DisplayName("Belgeler, rol bilgisi ve soru modele iletilir")
    void shouldPassKnowledgeRoleAndQuestion() {
        Actor warehouse = TestTokens.actor("warehouse");

        String answer = service.ask(new AskHelpCommand(warehouse, "Çuval nasıl eklenir?", List.of()));

        assertThat(answer).isEqualTo("cevap");
        assertThat(model.knowledge).isEqualTo("BELGELER");
        assertThat(model.userContext).contains("Depo").contains("Bugünün tarihi: 2026-11-18");
        assertThat(model.conversation).containsExactly(new ChatMessage(Speaker.USER, "Çuval nasıl eklenir?"));
    }

    @Test
    @DisplayName("Geçmişin yalnızca son mesajları, sırayla ve geçerli olanlar iletilir")
    void shouldKeepRecentValidHistory() {
        List<ChatMessage> history = new ArrayList<>();
        for (int i = 0; i < PAIRS; i++) {
            history.add(new ChatMessage(Speaker.USER, "soru " + i));
            history.add(new ChatMessage(Speaker.ASSISTANT, "cevap " + i));
        }

        service.ask(new AskHelpCommand(TestTokens.actor("manager"), "yeni soru", history));

        assertThat(model.conversation).hasSize(AskHelpService.MAX_HISTORY_MESSAGES + 1);
        assertThat(model.conversation.getFirst().speaker()).isEqualTo(Speaker.USER);
        assertThat(model.conversation.getLast().text()).isEqualTo("yeni soru");
    }

    @Test
    @DisplayName("Cevapsız kalan son kullanıcı mesajı atılır")
    void shouldDropUnansweredTrailingUserMessage() {
        List<ChatMessage> history = List.of(new ChatMessage(Speaker.USER, "eski soru"));

        service.ask(new AskHelpCommand(TestTokens.actor("manager"), "yeni soru", history));

        assertThat(model.conversation).containsExactly(new ChatMessage(Speaker.USER, "yeni soru"));
    }

    @Test
    @DisplayName("Geçici şifresini değiştirmemiş (rolsüz) kişinin sorusu modele gitmez")
    void shouldRejectActorWithoutRole() {
        Actor withoutRole = TestTokens.actor();

        assertThatThrownBy(() -> service.ask(new AskHelpCommand(withoutRole, "soru", List.of())))
                .isInstanceOf(HelpUnavailableException.class);
        assertThat(model.conversation).isEmpty();
    }

    @Test
    @DisplayName("Anahtar tanımlı değilse asistan kapalıdır ve soru reddedilir")
    void shouldBeUnavailableWithoutKey() {
        AskHelpService closed =
                new AskHelpService(() -> "BELGELER", new RecordingModel(false), new QuestionRateLimiter(CLOCK), CLOCK);

        assertThat(closed.isAvailable()).isFalse();
        assertThatThrownBy(() -> closed.ask(new AskHelpCommand(TestTokens.actor("manager"), "soru", List.of())))
                .isInstanceOf(HelpUnavailableException.class);
    }

    @Test
    @DisplayName("Saatlik soru sınırı dolunca soru reddedilir")
    void shouldRateLimitPerUser() {
        Actor actor = TestTokens.actor("manager");
        for (int i = 0; i < QuestionRateLimiter.MAX_QUESTIONS; i++) {
            service.ask(new AskHelpCommand(actor, "soru", List.of()));
        }

        assertThatThrownBy(() -> service.ask(new AskHelpCommand(actor, "soru", List.of())))
                .isInstanceOf(HelpRateLimitedException.class);
        // Başka kullanıcının hakkı ayrıdır.
        assertThat(service.ask(new AskHelpCommand(TestTokens.actor("quality"), "soru", List.of())))
                .isEqualTo("cevap");
    }

    @Test
    @DisplayName("Boş ya da çok uzun soru geçersizdir")
    void shouldRejectInvalidQuestion() {
        Actor actor = TestTokens.actor("manager");

        assertThatThrownBy(() -> new AskHelpCommand(actor, " ", List.of()))
                .isInstanceOf(InvalidHelpQuestionException.class);
        assertThatThrownBy(
                        () -> new AskHelpCommand(actor, "a".repeat(AskHelpCommand.MAX_QUESTION_LENGTH + 1), List.of()))
                .isInstanceOf(InvalidHelpQuestionException.class);
    }

    private static final class RecordingModel implements HelpModelPort {

        private final boolean configured;
        private String knowledge = "";
        private String userContext = "";
        private List<ChatMessage> conversation = List.of();

        RecordingModel(boolean configured) {
            this.configured = configured;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public String complete(String documents, String context, List<ChatMessage> messages) {
            this.knowledge = documents;
            this.userContext = context;
            this.conversation = messages;
            return "cevap";
        }
    }
}
