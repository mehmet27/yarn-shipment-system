package com.yarnshipment.assistant.adapter.out.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicException;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.models.messages.TextBlockParam;
import com.yarnshipment.assistant.application.port.out.HelpModelPort;
import com.yarnshipment.assistant.domain.model.ChatMessage;
import com.yarnshipment.assistant.domain.model.HelpUnavailableException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cevabı Claude Haiku'dan alır. Değişmeyen kurallar ve belgeler sistem isteminin ilk bloğudur ve önbelleğe alınır
 * (sonraki sorularda yeniden ücretlendirilmez); kullanıcıya özel bilgi önbellek noktasından sonra gelir. Düşük çaba
 * ve sınırlı çıktı uzunluğu maliyeti düşük tutar. Soru ve cevap metni loglanmaz.
 */
@Component
final class AnthropicHelpModelAdapter implements HelpModelPort {

    private static final Logger LOG = LoggerFactory.getLogger(AnthropicHelpModelAdapter.class);

    private final HelpProperties properties;
    private @Nullable AnthropicClient client;

    AnthropicHelpModelAdapter(HelpProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isConfigured() {
        return properties.hasApiKey();
    }

    @Override
    public String complete(String knowledge, String userContext, List<ChatMessage> conversation) {
        if (!isConfigured()) {
            throw new HelpUnavailableException("ANTHROPIC_API_KEY is not set");
        }
        try {
            Message response = client().messages().create(request(knowledge, userContext, conversation));
            LOG.debug(
                    "Help answer: input={} output={} cacheRead={} cacheWrite={}",
                    response.usage().inputTokens(),
                    response.usage().outputTokens(),
                    response.usage().cacheReadInputTokens().orElse(0L),
                    response.usage().cacheCreationInputTokens().orElse(0L));
            return textOf(response);
        } catch (AnthropicException e) {
            LOG.warn("Help assistant model call failed: {}", e.getClass().getSimpleName());
            throw new HelpUnavailableException("model call failed", e);
        }
    }

    private MessageCreateParams request(String knowledge, String userContext, List<ChatMessage> conversation) {
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(properties.model())
                .maxTokens(properties.maxOutputTokens())
                .outputConfig(
                        OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .systemOfTextBlockParams(List.of(
                        TextBlockParam.builder()
                                .text(HelpPrompt.system(knowledge))
                                .cacheControl(CacheControlEphemeral.builder()
                                        .ttl(CacheControlEphemeral.Ttl.TTL_1H)
                                        .build())
                                .build(),
                        TextBlockParam.builder().text(userContext).build()));
        for (ChatMessage message : conversation) {
            switch (message.speaker()) {
                case USER -> builder.addUserMessage(message.text());
                case ASSISTANT -> builder.addAssistantMessage(message.text());
            }
        }
        return builder.build();
    }

    private static String textOf(Message response) {
        if (response.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
            throw new HelpUnavailableException("model declined to answer");
        }
        String text = response.content().stream()
                .map(ContentBlock::text)
                .flatMap(Optional::stream)
                .map(TextBlock::text)
                .collect(Collectors.joining("\n"))
                .strip();
        if (text.isEmpty()) {
            throw new HelpUnavailableException("model returned no text");
        }
        return text;
    }

    private synchronized AnthropicClient client() {
        AnthropicClient current = client;
        if (current == null) {
            AnthropicOkHttpClient.Builder builder =
                    AnthropicOkHttpClient.builder().apiKey(properties.apiKey()).timeout(properties.timeout());
            if (!properties.baseUrl().isBlank()) {
                builder.baseUrl(properties.baseUrl());
            }
            current = builder.build();
            client = current;
        }
        return current;
    }
}
