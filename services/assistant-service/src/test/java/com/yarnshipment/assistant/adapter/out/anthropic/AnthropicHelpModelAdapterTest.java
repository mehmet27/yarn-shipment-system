package com.yarnshipment.assistant.adapter.out.anthropic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import com.yarnshipment.assistant.domain.model.ChatMessage;
import com.yarnshipment.assistant.domain.model.HelpUnavailableException;
import com.yarnshipment.assistant.domain.model.Speaker;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Model çağrısı sahte bir yerel sunucuya karşı denenir; gerçek API'ye gidilmez ve anahtar harcanmaz. */
class AnthropicHelpModelAdapterTest {

    private static final long MAX_OUTPUT = 512L;
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int OK = 200;
    private static final int BAD_REQUEST = 400;

    private @Nullable HttpServer server;
    private final AtomicReference<String> requestBody = new AtomicReference<>("");

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Belgeler önbellek işaretiyle, kullanıcı bilgisi ayrı blokta gider ve cevap metni döner")
    void shouldSendCachedKnowledgeAndReturnText() throws IOException {
        AnthropicHelpModelAdapter adapter = adapterAnswering(OK, message("Çuvalı Depo ekler.", "end_turn"));

        String answer = adapter.complete(
                "BELGELER",
                "Rol: Depo",
                List.of(
                        new ChatMessage(Speaker.USER, "Merhaba"),
                        new ChatMessage(Speaker.ASSISTANT, "Merhaba!"),
                        new ChatMessage(Speaker.USER, "Çuval kim ekler?")));

        assertThat(answer).isEqualTo("Çuvalı Depo ekler.");
        String sent = requestBody.get();
        assertThat(sent)
                .contains("\"model\":\"claude-haiku-5-5\"")
                .contains("\"max_tokens\":" + MAX_OUTPUT)
                .contains("BELGELER")
                .contains("Rol: Depo")
                .contains("\"cache_control\"")
                .contains("\"effort\":\"low\"")
                .contains("Çuval kim ekler?");
        // Önbellek noktası yalnızca değişmeyen ilk bloktadır; kullanıcı bilgisi ondan sonra gelir.
        assertThat(sent.indexOf("\"cache_control\"")).isLessThan(sent.indexOf("Rol: Depo"));
    }

    @Test
    @DisplayName("Model reddederse cevap verilemez sayılır")
    void shouldTreatRefusalAsUnavailable() throws IOException {
        AnthropicHelpModelAdapter adapter = adapterAnswering(OK, message("", "refusal"));

        assertThatThrownBy(() -> adapter.complete("B", "R", List.of(new ChatMessage(Speaker.USER, "soru"))))
                .isInstanceOf(HelpUnavailableException.class);
    }

    @Test
    @DisplayName("Metin dönmezse cevap verilemez sayılır")
    void shouldTreatEmptyTextAsUnavailable() throws IOException {
        AnthropicHelpModelAdapter adapter = adapterAnswering(OK, message("  ", "end_turn"));

        assertThatThrownBy(() -> adapter.complete("B", "R", List.of(new ChatMessage(Speaker.USER, "soru"))))
                .isInstanceOf(HelpUnavailableException.class);
    }

    @Test
    @DisplayName("Model servisi hata dönerse cevap verilemez sayılır")
    void shouldTreatApiErrorAsUnavailable() throws IOException {
        AnthropicHelpModelAdapter adapter = adapterAnswering(
                BAD_REQUEST, "{\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"message\":\"x\"}}");

        assertThatThrownBy(() -> adapter.complete("B", "R", List.of(new ChatMessage(Speaker.USER, "soru"))))
                .isInstanceOf(HelpUnavailableException.class);
    }

    @Test
    @DisplayName("Anahtar yoksa asistan kapalıdır ve model çağrılmaz")
    void shouldBeClosedWithoutApiKey() {
        AnthropicHelpModelAdapter adapter =
                new AnthropicHelpModelAdapter(new HelpProperties("", "claude-haiku-5-5", MAX_OUTPUT, TIMEOUT, ""));

        assertThat(adapter.isConfigured()).isFalse();
        assertThatThrownBy(() -> adapter.complete("B", "R", List.of(new ChatMessage(Speaker.USER, "soru"))))
                .isInstanceOf(HelpUnavailableException.class);
        assertThat(new HelpProperties("gizli-anahtar", "m", 1L, Duration.ofSeconds(1), "").toString())
                .doesNotContain("gizli-anahtar");
    }

    private AnthropicHelpModelAdapter adapterAnswering(int status, String body) throws IOException {
        HttpServer created = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server = created;
        created.createContext("/v1/messages", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        String baseUrl = "http://" + server.getAddress().getHostString() + ":"
                + server.getAddress().getPort();
        return new AnthropicHelpModelAdapter(
                new HelpProperties("test-key", "claude-haiku-5-5", MAX_OUTPUT, TIMEOUT, baseUrl));
    }

    private static String message(String text, String stopReason) {
        return "{\"id\":\"msg_1\",\"type\":\"message\",\"role\":\"assistant\",\"model\":\"claude-haiku-5-5\","
                + "\"content\":[{\"type\":\"text\",\"text\":\"" + text + "\"}],\"stop_reason\":\"" + stopReason + "\","
                + "\"stop_sequence\":null,\"usage\":{\"input_tokens\":10,\"output_tokens\":5,"
                + "\"cache_creation_input_tokens\":0,\"cache_read_input_tokens\":0}}";
    }
}
