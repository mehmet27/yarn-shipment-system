package com.yarnshipment.discrepancy.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.discrepancy.TestDiscrepancies;
import com.yarnshipment.discrepancy.TestTickets;
import com.yarnshipment.discrepancy.TestTokens;
import com.yarnshipment.discrepancy.application.port.in.ticket.GetDiscrepancyTicketsUseCase;
import com.yarnshipment.discrepancy.application.port.in.ticket.MarkTicketMisreadCommand;
import com.yarnshipment.discrepancy.application.port.in.ticket.MarkTicketMisreadUseCase;
import com.yarnshipment.discrepancy.domain.ticket.ReviewedTicket;
import com.yarnshipment.discrepancy.domain.ticket.TicketMisread;
import com.yarnshipment.discrepancy.domain.ticket.TicketReading;
import com.yarnshipment.discrepancy.domain.ticket.TicketReadingNotFoundException;
import com.yarnshipment.discrepancy.domain.ticket.TicketSide;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Fiş paneli uçlarının sözleşmesi, hata kodu ve izin denetimi. */
@WebMvcTest(controllers = DiscrepancyTicketController.class)
class DiscrepancyTicketControllerTest {

    private static final String TICKETS = "/api/v1/discrepancies/" + TestDiscrepancies.ID + "/tickets";

    private final MockMvcTester mvc;

    @MockitoBean
    private GetDiscrepancyTicketsUseCase getTickets;

    @MockitoBean
    private MarkTicketMisreadUseCase markMisread;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    DiscrepancyTicketControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName(
            "Okunan kilolar ve elle girilen net ondalık dize döner; inceleyen işaretleyebilir, fasoncu işaretleyemez")
    void shouldListTickets() {
        TicketReading read = TestTickets.reading(TicketSide.DISPATCH_SCALE, TestTickets.weights());
        when(getTickets.getTickets(any()))
                .thenReturn(List.of(new ReviewedTicket(read, new BigDecimal("1990.000"), null)));

        MvcTestResult result = get(TestTokens.token("quality").build());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$[0].status").isEqualTo("READ");
        assertThat(result).bodyJson().extractingPath("$[0].netKg").isEqualTo("2000.000");
        assertThat(result).bodyJson().extractingPath("$[0].enteredNetKg").isEqualTo("1990.000");
        assertThat(result).bodyJson().extractingPath("$[0].allowedActions[0]").isEqualTo("MARK_MISREAD");

        MvcTestResult manager = get(TestTokens.token("manager").build());
        assertThat(manager).hasStatusOk();
        assertThat(manager)
                .bodyJson()
                .extractingPath("$[0].allowedActions")
                .asArray()
                .isEmpty();
    }

    @Test
    @DisplayName("İşaretli okuma kilosuz, işaretle ve eylemsiz döner; not komuta geçer")
    void shouldMarkMisread() {
        TicketReading unreadable = TestTickets.reading(TicketSide.DELIVERY_SCALE, null);
        TicketMisread misread = new TicketMisread(
                unreadable.evidenceId(), TestDiscrepancies.ID, UUID.randomUUID(), TestTickets.READ_AT, "brüt");
        when(markMisread.markMisread(any())).thenReturn(List.of(new ReviewedTicket(unreadable, null, misread)));

        MvcTestResult result = post(
                unreadable.evidenceId(),
                "{\"note\":\"brüt\"}",
                TestTokens.token("quality").build());

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().doesNotHavePath("$[0].netKg");
        assertThat(result).bodyJson().extractingPath("$[0].misread.note").isEqualTo("brüt");
        assertThat(result)
                .bodyJson()
                .extractingPath("$[0].allowedActions")
                .asArray()
                .isEmpty();
        ArgumentCaptor<MarkTicketMisreadCommand> command = ArgumentCaptor.forClass(MarkTicketMisreadCommand.class);
        verify(markMisread).markMisread(command.capture());
        assertThat(command.getValue().evidenceId()).isEqualTo(unreadable.evidenceId());
        assertThat(command.getValue().note()).isEqualTo("brüt");
    }

    @Test
    @DisplayName("Başka sevkiyatın okuması 404; karar yetkisi olmayan 403 alır")
    void shouldMapErrorsAndPermissions() {
        UUID evidenceId = UUID.randomUUID();
        when(markMisread.markMisread(any())).thenThrow(new TicketReadingNotFoundException(evidenceId));

        MvcTestResult missing =
                post(evidenceId, "{}", TestTokens.token("quality").build());
        assertThat(missing).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(missing).bodyJson().extractingPath("$.code").isEqualTo("TICKET_READING_NOT_FOUND");

        assertThat(post(evidenceId, "{}", TestTokens.token("warehouse").build()))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult get(Jwt jwt) {
        return mvc.get()
                .uri(TICKETS)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }

    private MvcTestResult post(UUID evidenceId, String body, Jwt jwt) {
        return mvc.post()
                .uri(TICKETS + "/" + evidenceId + "/misread")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }
}
