package com.yarnshipment.evidence.adapter.out.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anthropic.client.AnthropicClient;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.services.blocking.MessageService;
import com.yarnshipment.evidence.domain.ticket.ReaderVersion;
import com.yarnshipment.evidence.domain.ticket.ReadingProblem;
import com.yarnshipment.evidence.domain.ticket.TicketReaderUnavailableException;
import com.yarnshipment.evidence.domain.ticket.TicketScan;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class ClaudeTicketReaderTest {

    private static final String MODEL = "claude-sonnet-5-5";
    private static final long TIMEOUT_SECONDS = 30L;

    private final AnthropicClient client = mock(AnthropicClient.class);
    private final MessageService messages = mock(MessageService.class);
    private final ClaudeTicketReader reader = new ClaudeTicketReader(
            client, MODEL, new TicketScanParser(JsonMapper.builder().build()));

    private void givenAnswer(StopReason stopReason, String json) {
        TextBlock text = mock(TextBlock.class);
        when(text.text()).thenReturn(json);
        ContentBlock block = mock(ContentBlock.class);
        when(block.text()).thenReturn(Optional.of(text));
        Message message = mock(Message.class);
        when(message.stopReason()).thenReturn(Optional.of(stopReason));
        when(message.content()).thenReturn(List.of(block));
        when(client.messages()).thenReturn(messages);
        when(messages.create(any(MessageCreateParams.class))).thenReturn(message);
    }

    @Test
    @DisplayName("Fotoğraf JPEG olarak, şemaya bağlı çıktı istenerek gönderilir; cevap alanlara çevrilir")
    void shouldReadTicket() throws IOException {
        givenAnswer(StopReason.END_TURN, TicketScanParserTest.CLEAR);

        TicketScan scan = reader.scanTicket(
                TicketImagesTest.png(TicketImagesTest.SMALL_WIDTH, TicketImagesTest.SMALL_HEIGHT), "image/png");

        assertThat(scan.gross().kg()).isEqualByComparingTo(new BigDecimal("12480"));
        ArgumentCaptor<MessageCreateParams> request = ArgumentCaptor.forClass(MessageCreateParams.class);
        verify(messages).create(request.capture());
        String body = request.getValue().toString();
        assertThat(body).contains(MODEL, "image/jpeg", "photoProblems", TicketPrompt.USER);
        assertThat(reader.readerVersion()).isEqualTo(new ReaderVersion(MODEL, TicketPrompt.VERSION));
    }

    @Test
    @DisplayName("Çözülemeyen görüntü modele gönderilmez, biçim sorunu olarak döner")
    void shouldSkipUnsupportedImage() {
        TicketScan scan = reader.scanTicket(TicketImagesTest.NOT_AN_IMAGE, "image/heic");

        assertThat(scan.photoProblems()).containsExactly(ReadingProblem.UNSUPPORTED_FORMAT);
        verify(client, never()).messages();
    }

    @Test
    @DisplayName("Model reddederse, yarıda keserse ya da ulaşılamazsa okuma yapılamadı döner")
    void shouldReportUnavailable() throws IOException {
        byte[] image = TicketImagesTest.png(TicketImagesTest.SMALL_WIDTH, TicketImagesTest.SMALL_HEIGHT);
        givenAnswer(StopReason.REFUSAL, "");
        assertThatThrownBy(() -> reader.scanTicket(image, "image/png"))
                .isInstanceOf(TicketReaderUnavailableException.class);

        when(messages.create(any(MessageCreateParams.class))).thenThrow(new AnthropicIoException("timeout"));
        assertThatThrownBy(() -> reader.scanTicket(image, "image/png"))
                .isInstanceOf(TicketReaderUnavailableException.class);
    }

    @Test
    @DisplayName("Okuma kapalıyken her istek okuma yapılamadı döner; açıkken Claude okuyucusu kurulur")
    void shouldBuildReaderFromSettings() {
        TicketReaderConfiguration configuration = new TicketReaderConfiguration();
        JsonMapper jsonMapper = JsonMapper.builder().build();
        Duration timeout = Duration.ofSeconds(TIMEOUT_SECONDS);

        var disabled = configuration.ticketReader(new TicketReadingProperties(false, MODEL, "", timeout), jsonMapper);
        var enabled =
                configuration.ticketReader(new TicketReadingProperties(true, MODEL, "test-key", timeout), jsonMapper);

        assertThat(disabled.readerVersion().model()).isEqualTo("none");
        assertThatThrownBy(() -> disabled.scanTicket(new byte[] {1}, "image/jpeg"))
                .isInstanceOf(TicketReaderUnavailableException.class);
        assertThat(enabled).isInstanceOf(ClaudeTicketReader.class);
        assertThat(enabled.readerVersion().model()).isEqualTo(MODEL);
    }
}
