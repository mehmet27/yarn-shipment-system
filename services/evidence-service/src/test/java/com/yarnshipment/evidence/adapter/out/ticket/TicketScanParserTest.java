package com.yarnshipment.evidence.adapter.out.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.yarnshipment.evidence.domain.ticket.ReadConfidence;
import com.yarnshipment.evidence.domain.ticket.ReadingProblem;
import com.yarnshipment.evidence.domain.ticket.TicketFields;
import com.yarnshipment.evidence.domain.ticket.TicketReaderUnavailableException;
import com.yarnshipment.evidence.domain.ticket.TicketScan;
import com.yarnshipment.evidence.domain.ticket.WeightValue;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class TicketScanParserTest {

    static final String CLEAR = """
            {"gross":{"value":12480,"legible":true,"confidence":"HIGH"},
             "tare":{"value":8210.5,"legible":true,"confidence":"HIGH"},
             "net":{"value":null,"legible":false,"confidence":"LOW"},
             "plate":{"value":" 34 ABC 123 ","legible":true,"confidence":"HIGH"},
             "weighedAt":{"value":"2026-10-27T09:42","legible":true,"confidence":"MEDIUM"},
             "ticketNo":{"value":"004512","legible":true,"confidence":"HIGH"},
             "scaleName":{"value":"","legible":true,"confidence":"LOW"},
             "photoProblems":["GLARE","SMUDGE"]}""";

    private final TicketScanParser parser =
            new TicketScanParser(JsonMapper.builder().build());

    @Test
    @DisplayName("Okunan alanlar çevrilir; fiş saati Türkiye saatidir, boş metin ve şema dışı sorun yok sayılır")
    void shouldParseFields() {
        TicketScan scan = parser.parse(CLEAR);

        assertThat(scan.gross()).isEqualTo(new WeightValue(new BigDecimal("12480"), ReadConfidence.HIGH));
        assertThat(scan.tare().kg()).isEqualByComparingTo("8210.5");
        assertThat(scan.net()).isEqualTo(WeightValue.MISSING);
        assertThat(scan.fields())
                .isEqualTo(new TicketFields("34 ABC 123", Instant.parse("2026-10-27T06:42:00Z"), "004512", null));
        assertThat(scan.photoProblems()).containsExactly(ReadingProblem.GLARE);
        assertThat(scan.rawResponse()).isEqualTo(CLEAR);
    }

    @Test
    @DisplayName("Okunaksız işaretli değer boş sayılır; bilinmeyen eminlik düşük, bozuk tarih boş olur")
    void shouldDistrustIllegibleValues() {
        TicketScan scan = parser.parse("""
                {"gross":{"value":12480,"legible":false,"confidence":"HIGH"},
                 "tare":{"value":8210,"legible":true,"confidence":"SURE"},
                 "weighedAt":{"value":"27.10.2026","legible":true,"confidence":"HIGH"},
                 "plate":{"value":"34 ABC 123","legible":false,"confidence":"HIGH"}}""");

        assertThat(scan.gross()).isEqualTo(WeightValue.MISSING);
        assertThat(scan.tare().confidence()).isEqualTo(ReadConfidence.LOW);
        assertThat(scan.fields()).isEqualTo(TicketFields.EMPTY);
        assertThat(scan.photoProblems()).isEmpty();
    }

    @Test
    @DisplayName("JSON olmayan cevap okuma yapılamadı sayılır")
    void shouldRejectInvalidJson() {
        assertThatThrownBy(() -> parser.parse("not json")).isInstanceOf(TicketReaderUnavailableException.class);
    }
}
