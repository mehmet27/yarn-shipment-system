package com.yarnshipment.evidence.adapter.out.ticket;

import com.yarnshipment.evidence.domain.ticket.ReadConfidence;
import com.yarnshipment.evidence.domain.ticket.ReadingProblem;
import com.yarnshipment.evidence.domain.ticket.TicketFields;
import com.yarnshipment.evidence.domain.ticket.TicketReaderUnavailableException;
import com.yarnshipment.evidence.domain.ticket.TicketScan;
import com.yarnshipment.evidence.domain.ticket.WeightValue;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Modelin JSON cevabını {@link TicketScan}'e çevirir. Okunaksız işaretli alan değer taşısa bile boş sayılır; fiş
 * saati Türkiye saatidir.
 */
final class TicketScanParser {

    private static final ZoneId TICKET_ZONE = ZoneId.of("Europe/Istanbul");

    private final JsonMapper jsonMapper;

    TicketScanParser(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    TicketScan parse(String json) {
        JsonNode root;
        try {
            root = jsonMapper.readTree(json);
        } catch (JacksonException exception) {
            throw new TicketReaderUnavailableException("Ticket reader returned invalid JSON", exception);
        }
        return new TicketScan(
                weight(root.path("gross")),
                weight(root.path("tare")),
                weight(root.path("net")),
                new TicketFields(
                        text(root.path("plate")),
                        weighedAt(text(root.path("weighedAt"))),
                        text(root.path("ticketNo")),
                        text(root.path("scaleName"))),
                photoProblems(root.path("photoProblems")),
                json);
    }

    private static WeightValue weight(JsonNode field) {
        JsonNode value = field.path("value");
        if (!isLegible(field) || !value.isNumber()) {
            return WeightValue.MISSING;
        }
        return new WeightValue(value.decimalValue(), confidence(field.path("confidence")));
    }

    private static @Nullable String text(JsonNode field) {
        JsonNode value = field.path("value");
        if (!isLegible(field) || !value.isString() || value.stringValue().isBlank()) {
            return null;
        }
        return value.stringValue().strip();
    }

    private static boolean isLegible(JsonNode field) {
        return field.path("legible").asBoolean(false);
    }

    private static ReadConfidence confidence(JsonNode node) {
        try {
            return ReadConfidence.valueOf(node.asString(ReadConfidence.LOW.name()));
        } catch (IllegalArgumentException exception) {
            return ReadConfidence.LOW;
        }
    }

    private static @Nullable Instant weighedAt(@Nullable String printed) {
        if (printed == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(printed).atZone(TICKET_ZONE).toInstant();
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static List<ReadingProblem> photoProblems(JsonNode node) {
        List<ReadingProblem> problems = new ArrayList<>();
        for (JsonNode item : node.values()) {
            try {
                problems.add(ReadingProblem.valueOf(item.asString("")));
            } catch (IllegalArgumentException exception) {
                // Şema dışı değer yok sayılır; okuma kuralları kilo alanlarına bakar.
            }
        }
        return problems;
    }
}
