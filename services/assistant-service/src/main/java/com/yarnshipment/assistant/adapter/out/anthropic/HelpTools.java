package com.yarnshipment.assistant.adapter.out.anthropic;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.core.type.TypeReference;
import com.yarnshipment.assistant.application.port.out.HelpDataPort;
import com.yarnshipment.assistant.domain.model.HelpDataArgument;
import com.yarnshipment.assistant.domain.model.HelpDataSource;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Modele verilen tek araç: canlı veriyi salt okunur sorgulamak. Araç, tanımlı kaynaklardan birini adıyla seçer;
 * başka bir adres ya da yazma işlemi yoktur.
 */
final class HelpTools {

    static final String READ_DATA = "veri_oku";

    private static final String TYPE = "type";
    private static final String STRING = "string";
    private static final String DESCRIPTION = "description";

    private HelpTools() {}

    /** Aracın modele anlatılan tanımı. */
    static Tool readDataTool() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(
                "kaynak",
                Map.of(
                        TYPE,
                        STRING,
                        "enum",
                        Arrays.stream(HelpDataSource.values())
                                .map(HelpDataSource::toolName)
                                .toList(),
                        DESCRIPTION,
                        "Okunacak veri kaynağı"));
        for (HelpDataArgument argument : HelpDataArgument.values()) {
            properties.put(argument.toolName(), Map.of(TYPE, STRING, DESCRIPTION, argumentHelp(argument)));
        }
        Tool.InputSchema.Properties.Builder schemaProperties = Tool.InputSchema.Properties.builder();
        properties.forEach((name, schema) -> schemaProperties.putAdditionalProperty(name, JsonValue.from(schema)));
        return Tool.builder()
                .name(READ_DATA)
                .description(description())
                .inputSchema(Tool.InputSchema.builder()
                        .properties(schemaProperties.build())
                        .required(List.of("kaynak"))
                        .build())
                .build();
    }

    private static String description() {
        String sources = Arrays.stream(HelpDataSource.values())
                .map(source -> "- " + source.toolName() + ": " + source.description())
                .collect(Collectors.joining("\n"));
        return "Uygulamadaki canlı veriyi yalnızca okur (hiçbir şeyi değiştiremez). Sonuçlar soran kullanıcının "
                + "yetkisiyle gelir. Kaynaklar:\n" + sources
                + "\nTarihler YYYY-MM-DD yazılır. Dönem verilmezse son 30 gün kullanılır.";
    }

    private static String argumentHelp(HelpDataArgument argument) {
        return switch (argument) {
            case START -> "Dönem başı, YYYY-MM-DD";
            case END -> "Dönem sonu (dahil), YYYY-MM-DD";
            case SUBCONTRACTOR -> "Yalnızca bu fasoncunun kaydı (fasoncular kaynağından gelen kimlik)";
            case SEARCH -> "Arama metni";
            case STATUS -> "Sevkiyat durumu (DRAFT, WEIGHT_MISMATCH, AWAITING_DELIVERY, COMPLETED, CANCELLED)";
            case PAGE -> "Sayfa numarası, 0'dan başlar";
            case SHIPMENT_NO -> "Sevkiyat numarası, örn. SHP-0010";
        };
    }

    /** Modelin istediği aracı çalıştırır ve sonucu, modele geri verilecek blok olarak döner. */
    static ContentBlockParam toolResult(ToolUseBlock use, HelpDataPort data) {
        String result = resultFor(use, data);
        return ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                .toolUseId(use.id())
                .content(result)
                .build());
    }

    private static String resultFor(ToolUseBlock use, HelpDataPort data) {
        if (!READ_DATA.equals(use.name())) {
            return "Bilinmeyen araç.";
        }
        Map<String, Object> input = inputOf(use);
        String sourceName = textOf(input.get("kaynak"));
        return HelpDataSource.byToolName(sourceName)
                .map(source -> data.read(source, arguments(input)))
                .orElse("Bilinmeyen kaynak: " + sourceName);
    }

    private static Map<HelpDataArgument, String> arguments(Map<String, Object> input) {
        Map<HelpDataArgument, String> arguments = new EnumMap<>(HelpDataArgument.class);
        for (HelpDataArgument argument : HelpDataArgument.values()) {
            String value = textOf(input.get(argument.toolName()));
            if (!value.isBlank()) {
                arguments.put(argument, value);
            }
        }
        return arguments;
    }

    private static Map<String, Object> inputOf(ToolUseBlock use) {
        Map<String, Object> input = use._input().convert(new TypeReference<Map<String, Object>>() {});
        return input == null ? Map.of() : input;
    }

    private static String textOf(@Nullable Object value) {
        return value == null ? "" : value.toString();
    }
}
