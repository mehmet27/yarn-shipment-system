package com.yarnshipment.evidence.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.yarnshipment.evidence.TestTickets;
import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.in.ReadTicketUseCase;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.ticket.ReadingProblem;
import com.yarnshipment.evidence.domain.ticket.TicketReaderUnavailableException;
import com.yarnshipment.evidence.domain.ticket.TicketReading;
import com.yarnshipment.evidence.domain.ticket.TicketReadingNotSupportedException;
import com.yarnshipment.evidence.domain.ticket.TicketScan;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Fiş okuma ucunun sözleşmesi, hata kodları ve izin denetimi. */
@WebMvcTest(controllers = TicketReadingController.class)
class TicketReadingControllerTest {

    private static final String URI = "/api/v1/evidence/" + TestUploads.EVIDENCE_ID + "/ticket-reading";

    private final MockMvcTester mvc;

    @MockitoBean
    private ReadTicketUseCase readTicket;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    TicketReadingControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Okunan fiş brüt, dara, net ve alanlarla döner")
    void shouldReturnReading() {
        when(readTicket.readTicket(any())).thenReturn(reading(TestTickets.clearScan()));

        MvcTestResult result = post(TestTokens.token("warehouse").build());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("READ");
        assertThat(result).bodyJson().extractingPath("$.netKg").isEqualTo(TestTickets.NET.intValueExact());
        assertThat(result).bodyJson().extractingPath("$.plate").isEqualTo(TestTickets.PLATE);
        assertThat(result).bodyJson().extractingPath("$.weighedAt").isEqualTo("2026-10-27T09:00:00Z");
    }

    @Test
    @DisplayName("Okunamayan fişte kilolar boş, sorunlar listelenir")
    void shouldReturnUnreadable() {
        when(readTicket.readTicket(any()))
                .thenReturn(reading(TicketScan.unreadableImage(ReadingProblem.UNSUPPORTED_FORMAT)));

        MvcTestResult result = post(TestTokens.subcontractorToken());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("UNREADABLE");
        assertThat(result).bodyJson().doesNotHavePath("$.grossKg");
        assertThat(result).bodyJson().extractingPath("$.problems[0]").isEqualTo("WEIGHT_NOT_READ");
    }

    @Test
    @DisplayName("Okuma kapalıysa 503, fiş olmayan kanıt 400; yükleme izni olmayan 403 alır")
    void shouldMapErrorsAndPermissions() {
        when(readTicket.readTicket(any())).thenThrow(new TicketReaderUnavailableException("disabled"));
        MvcTestResult unavailable = post(TestTokens.token("warehouse").build());
        assertThat(unavailable).hasStatus(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(unavailable).bodyJson().extractingPath("$.code").isEqualTo("TICKET_READING_UNAVAILABLE");

        doThrow(new TicketReadingNotSupportedException(UUID.randomUUID()))
                .when(readTicket)
                .readTicket(any());
        MvcTestResult notSupported = post(TestTokens.token("warehouse").build());
        assertThat(notSupported).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(notSupported).bodyJson().extractingPath("$.code").isEqualTo("TICKET_READING_NOT_SUPPORTED");

        assertThat(post(TestTokens.token("manager").build())).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(post(TestTokens.driverToken())).hasStatus(HttpStatus.FORBIDDEN);
    }

    private static TicketReading reading(TicketScan scan) {
        return TicketReading.evaluate(
                TestTickets.file(UUID.randomUUID(), EvidenceType.DISPATCH_SCALE),
                scan,
                TestTickets.VERSION,
                TestUploads.NOW);
    }

    private MvcTestResult post(Jwt jwt) {
        return mvc.post()
                .uri(URI)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }
}
