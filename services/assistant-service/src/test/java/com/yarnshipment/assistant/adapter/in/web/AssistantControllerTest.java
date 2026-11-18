package com.yarnshipment.assistant.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.assistant.TestTokens;
import com.yarnshipment.assistant.application.port.in.AskHelpCommand;
import com.yarnshipment.assistant.application.port.in.AskHelpUseCase;
import com.yarnshipment.assistant.application.port.in.GetHelpStatusUseCase;
import com.yarnshipment.assistant.domain.model.HelpRateLimitedException;
import com.yarnshipment.assistant.domain.model.HelpUnavailableException;
import com.yarnshipment.assistant.domain.model.Speaker;
import com.yarnshipment.common.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Yardım uçlarının sözleşmesi, hata kodları ve giriş denetimi. */
@WebMvcTest(controllers = AssistantController.class)
class AssistantControllerTest {

    private static final String ASK = "/api/v1/assistant/ask";

    private final MockMvcTester mvc;

    @MockitoBean
    private AskHelpUseCase askHelp;

    @MockitoBean
    private GetHelpStatusUseCase getStatus;

    @Autowired
    AssistantControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Soru kullanıcının rolü ve önceki mesajlarla birlikte cevaplanır")
    void shouldAnswerQuestionWithActorAndHistory() {
        when(askHelp.ask(any())).thenReturn("Çuvalları Depo ekler.");

        MvcTestResult result = post(ASK, """
                {"question":"Çuval nasıl eklenir?","history":[
                  {"role":"user","text":"Merhaba"},{"role":"assistant","text":"Merhaba, nasıl yardımcı olabilirim?"}]}
                """);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.answer").isEqualTo("Çuvalları Depo ekler.");
        ArgumentCaptor<AskHelpCommand> command = ArgumentCaptor.forClass(AskHelpCommand.class);
        verify(askHelp).ask(command.capture());
        assertThat(command.getValue().actor().hasRole(Role.WAREHOUSE)).isTrue();
        assertThat(command.getValue().question()).isEqualTo("Çuval nasıl eklenir?");
        assertThat(command.getValue().history()).hasSize(2);
        assertThat(command.getValue().history().get(1).speaker()).isEqualTo(Speaker.ASSISTANT);
    }

    @Test
    @DisplayName("Boş soru HELP_QUESTION_INVALID koduyla 400 döner")
    void shouldRejectBlankQuestion() {
        MvcTestResult result = post(ASK, "{\"question\":\"  \"}");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("HELP_QUESTION_INVALID");
    }

    @Test
    @DisplayName("Soru sınırı dolunca HELP_RATE_LIMITED koduyla 429 döner")
    void shouldReturnTooManyRequests() {
        when(askHelp.ask(any())).thenThrow(new HelpRateLimitedException("limit"));

        MvcTestResult result = post(ASK, "{\"question\":\"Merhaba\"}");

        assertThat(result).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("HELP_RATE_LIMITED");
    }

    @Test
    @DisplayName("Model cevap veremezse HELP_UNAVAILABLE koduyla 503 döner")
    void shouldReturnServiceUnavailable() {
        when(askHelp.ask(any())).thenThrow(new HelpUnavailableException("down"));

        MvcTestResult result = post(ASK, "{\"question\":\"Merhaba\"}");

        assertThat(result).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("HELP_UNAVAILABLE");
    }

    @Test
    @DisplayName("Durum ucu asistanın hazır olup olmadığını söyler")
    void shouldReportStatus() {
        when(getStatus.isAvailable()).thenReturn(true);

        MvcTestResult result = mvc.get()
                .uri("/api/v1/assistant/status")
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(
                        TestTokens.token("warehouse").build())))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.available").isEqualTo(true);
    }

    @Test
    @DisplayName("Giriş yapmamış kullanıcı 401 alır")
    void shouldRejectAnonymous() {
        assertThat(mvc.post()
                        .uri(ASK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"a\"}")
                        .exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult post(String uri, String body) {
        return mvc.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(
                        TestTokens.token("warehouse").build())))
                .exchange();
    }
}
