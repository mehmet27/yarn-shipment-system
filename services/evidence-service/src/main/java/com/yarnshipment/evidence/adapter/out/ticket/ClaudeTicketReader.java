package com.yarnshipment.evidence.adapter.out.ticket;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.yarnshipment.evidence.application.port.out.TicketReaderPort;
import com.yarnshipment.evidence.domain.ticket.ReaderVersion;
import com.yarnshipment.evidence.domain.ticket.ReadingProblem;
import com.yarnshipment.evidence.domain.ticket.TicketReaderUnavailableException;
import com.yarnshipment.evidence.domain.ticket.TicketScan;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Claude ile fiş okuma. Cevap yapılandırılmış çıktıyla şemaya bağlanır; düşük efor seçilir çünkü iş okumadır ve
 * kullanıcı önizleme ekranında bekler. Model hatası ya da reddi "okuma yapılamadı" olarak döner, akışı durdurmaz.
 */
final class ClaudeTicketReader implements TicketReaderPort {

    private static final long MAX_TOKENS = 4_000L;

    private final AnthropicClient client;
    private final ReaderVersion version;
    private final TicketScanParser parser;
    private final OutputConfig outputConfig;

    ClaudeTicketReader(AnthropicClient client, String model, TicketScanParser parser) {
        this.client = client;
        this.version = new ReaderVersion(model, TicketPrompt.VERSION);
        this.parser = parser;
        JsonOutputFormat.Schema.Builder schema = JsonOutputFormat.Schema.builder();
        TicketPrompt.schema().forEach((key, value) -> schema.putAdditionalProperty(key, JsonValue.from(value)));
        this.outputConfig = OutputConfig.builder()
                .effort(OutputConfig.Effort.LOW)
                .format(JsonOutputFormat.builder().schema(schema.build()).build())
                .build();
    }

    @Override
    public ReaderVersion readerVersion() {
        return version;
    }

    @Override
    public TicketScan scanTicket(byte[] image, String mimeType) {
        Optional<byte[]> jpeg = TicketImages.toModelJpeg(image);
        if (jpeg.isEmpty()) {
            return TicketScan.unreadableImage(ReadingProblem.UNSUPPORTED_FORMAT);
        }
        Message message;
        try {
            message = client.messages().create(request(jpeg.get()));
        } catch (AnthropicException exception) {
            throw new TicketReaderUnavailableException("Ticket reader call failed", exception);
        }
        StopReason stopReason = message.stopReason().orElse(null);
        if (!StopReason.END_TURN.equals(stopReason)) {
            throw new TicketReaderUnavailableException("Ticket reader stopped with " + stopReason);
        }
        return parser.parse(text(message));
    }

    private MessageCreateParams request(byte[] jpeg) {
        return MessageCreateParams.builder()
                .model(version.model())
                .maxTokens(MAX_TOKENS)
                .system(TicketPrompt.SYSTEM)
                .outputConfig(outputConfig)
                .addUserMessageOfContentBlockParams(List.of(
                        ContentBlockParam.ofImage(Base64ImageSource.builder()
                                .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                                .data(Base64.getEncoder().encodeToString(jpeg))
                                .build()),
                        ContentBlockParam.ofText(TicketPrompt.USER)))
                .build();
    }

    private static String text(Message message) {
        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining());
    }
}
