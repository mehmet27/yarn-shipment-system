package com.yarnshipment.evidence.adapter.out.ticket;

import java.util.List;
import java.util.Map;

/**
 * Modele giden istem ve cevap şeması. Şema yapılandırılmış çıktıyla zorlanır; model serbest metin döndüremez. İstem
 * veya şema değişirse {@link #VERSION} artırılır, okumayla birlikte saklanır.
 */
final class TicketPrompt {

    static final String VERSION = "ticket-v1";

    static final String SYSTEM = """
            You read photos of weighbridge tickets (kantar fişi) from Turkish yarn warehouses and subcontractors.
            Report exactly what is printed on the ticket. Never guess, estimate or compute a value that is not \
            printed. If a field is missing, cut off, blurred, covered by glare or you are not sure of every digit, \
            set "legible" to false and "value" to null.

            - gross: brüt / dolu / toplam ağırlık, in kg.
            - tare: dara / boş ağırlık, in kg.
            - net: net ağırlık, in kg, only if printed.
            Weights are in kilograms. Turkish tickets may use "." as a thousands separator and "," as the decimal \
            separator: "12.480" means 12480 kg. Return weights as plain numbers.
            - confidence is HIGH only if every digit is clearly readable.
            - plate: vehicle plate as printed, e.g. "34 ABC 123".
            - weighedAt: date and time of weighing as printed, ISO 8601 local time "YYYY-MM-DDTHH:MM".
            - ticketNo: ticket or receipt number.
            - scaleName: weighbridge or company name in the header.
            - photoProblems: BLURRY, GLARE, CROPPED (part of the ticket is outside the photo) or NOT_A_TICKET.
            """;

    static final String USER = "Read this weighbridge ticket.";

    private TicketPrompt() {}

    /** Cevabın JSON şeması. */
    static Map<String, Object> schema() {
        Map<String, Object> weight = field(List.of("number", "null"));
        Map<String, Object> text = field(List.of("string", "null"));
        return Map.of(
                "type",
                "object",
                "additionalProperties",
                false,
                "required",
                List.of("gross", "tare", "net", "plate", "weighedAt", "ticketNo", "scaleName", "photoProblems"),
                "properties",
                Map.of(
                        "gross", weight,
                        "tare", weight,
                        "net", weight,
                        "plate", text,
                        "weighedAt", text,
                        "ticketNo", text,
                        "scaleName", text,
                        "photoProblems",
                                Map.of(
                                        "type",
                                        "array",
                                        "items",
                                        Map.of(
                                                "type",
                                                "string",
                                                "enum",
                                                List.of("BLURRY", "GLARE", "CROPPED", "NOT_A_TICKET")))));
    }

    private static Map<String, Object> field(List<String> valueTypes) {
        return Map.of(
                "type",
                "object",
                "additionalProperties",
                false,
                "required",
                List.of("value", "legible", "confidence"),
                "properties",
                Map.of(
                        "value", Map.of("type", valueTypes),
                        "legible", Map.of("type", "boolean"),
                        "confidence", Map.of("type", "string", "enum", List.of("HIGH", "MEDIUM", "LOW"))));
    }
}
