package com.yarnshipment.assistant.adapter.in.web;

import com.yarnshipment.assistant.application.port.in.AskHelpCommand;
import com.yarnshipment.assistant.application.port.in.AskHelpUseCase;
import com.yarnshipment.assistant.application.port.in.GetHelpStatusUseCase;
import com.yarnshipment.assistant.domain.model.ChatMessage;
import com.yarnshipment.assistant.domain.model.Speaker;
import com.yarnshipment.common.security.CurrentActorProvider;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Yardım asistanı uçları. Giriş yapmış her kullanıcı kullanır; asistan yalnızca bilgi verir. Method security
 * proxy'si için final değildir (SEC-02).
 */
@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AskHelpUseCase askHelp;
    private final GetHelpStatusUseCase getStatus;
    private final CurrentActorProvider currentActorProvider;

    public AssistantController(
            AskHelpUseCase askHelp, GetHelpStatusUseCase getStatus, CurrentActorProvider currentActorProvider) {
        this.askHelp = askHelp;
        this.getStatus = getStatus;
        this.currentActorProvider = currentActorProvider;
    }

    /** Balonun gösterilip gösterilmeyeceği: anahtar tanımlı değilse kapalıdır. */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<StatusView> status() {
        return ResponseEntity.ok(new StatusView(getStatus.isAvailable()));
    }

    @PostMapping("/ask")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AnswerView> ask(@RequestBody AskRequest request) {
        List<ChatMessage> history = request.history() == null
                ? List.of()
                : request.history().stream().map(AssistantController::toMessage).toList();
        String answer = askHelp.ask(new AskHelpCommand(
                currentActorProvider.getCurrentActor(), request.question() == null ? "" : request.question(), history));
        return ResponseEntity.ok(new AnswerView(answer));
    }

    private static ChatMessage toMessage(HistoryItem item) {
        return new ChatMessage("assistant".equals(item.role()) ? Speaker.ASSISTANT : Speaker.USER, item.text());
    }

    /**
     * Soru isteği.
     *
     * @param question yeni soru
     * @param history önceki mesajlar, eskiden yeniye
     */
    public record AskRequest(
            @Nullable String question, @Nullable List<HistoryItem> history) {}

    /**
     * Önceki mesaj.
     *
     * @param role {@code user} ya da {@code assistant}
     * @param text mesaj metni
     */
    public record HistoryItem(String role, String text) {}

    /**
     * Cevap.
     *
     * @param answer cevap metni
     */
    public record AnswerView(String answer) {}

    /**
     * Asistan durumu.
     *
     * @param available kullanıma hazır mı
     */
    public record StatusView(boolean available) {}
}
