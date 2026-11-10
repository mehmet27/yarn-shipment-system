package com.yarnshipment.evidence.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yarnshipment.evidence.TestTokens;
import com.yarnshipment.evidence.TestUploads;
import com.yarnshipment.evidence.application.port.in.DeclareTicketUnverifiedCommand;
import com.yarnshipment.evidence.application.port.in.DeclareTicketUnverifiedUseCase;
import com.yarnshipment.evidence.application.port.in.ReadTicketUseCase;
import com.yarnshipment.evidence.domain.model.EvidenceType;
import com.yarnshipment.evidence.domain.ticket.TicketNotUnreadableException;
import com.yarnshipment.evidence.domain.ticket.TicketUnverified;
import com.yarnshipment.evidence.domain.ticket.UnverifiedReason;
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

/** "Doğrulanamadı" ucunun sözleşmesi, hata kodu ve izin denetimi. */
@WebMvcTest(controllers = TicketReadingController.class)
class TicketUnverifiedControllerTest {

    private static final String URI = "/api/v1/evidence/" + TestUploads.EVIDENCE_ID + "/ticket-reading/unverified";
    private static final String BODY = "{\"reason\":\"TICKET_DAMAGED\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private ReadTicketUseCase readTicket;

    @MockitoBean
    private DeclareTicketUnverifiedUseCase declareUnverified;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    TicketUnverifiedControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Gerekçe kaydedilir; cevap kanıt, tür, gerekçe ve zamanla döner")
    void shouldDeclareUnverified() {
        when(declareUnverified.declareUnverified(any()))
                .thenReturn(new TicketUnverified(
                        TestUploads.EVIDENCE_ID,
                        UUID.randomUUID(),
                        EvidenceType.DISPATCH_SCALE,
                        UnverifiedReason.TICKET_DAMAGED,
                        TestUploads.NOW));

        MvcTestResult result = post(TestTokens.token("warehouse").build(), BODY);

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.reason").isEqualTo("TICKET_DAMAGED");
        assertThat(result).bodyJson().extractingPath("$.evidenceType").isEqualTo("DISPATCH_SCALE");
        ArgumentCaptor<DeclareTicketUnverifiedCommand> command =
                ArgumentCaptor.forClass(DeclareTicketUnverifiedCommand.class);
        verify(declareUnverified).declareUnverified(command.capture());
        assertThat(command.getValue().evidenceId()).isEqualTo(TestUploads.EVIDENCE_ID);
        assertThat(command.getValue().reason()).isEqualTo(UnverifiedReason.TICKET_DAMAGED);
    }

    @Test
    @DisplayName("Okunamamış fiş yoksa 409, bilinmeyen gerekçe 400; yükleme izni olmayan 403 alır")
    void shouldMapErrorsAndPermissions() {
        when(declareUnverified.declareUnverified(any()))
                .thenThrow(new TicketNotUnreadableException(TestUploads.EVIDENCE_ID));
        MvcTestResult conflict = post(TestTokens.token("warehouse").build(), BODY);
        assertThat(conflict).hasStatus(HttpStatus.CONFLICT);
        assertThat(conflict).bodyJson().extractingPath("$.code").isEqualTo("TICKET_NOT_UNREADABLE");

        assertThat(post(TestTokens.token("warehouse").build(), "{\"reason\":\"LOST\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(post(TestTokens.token("manager").build(), BODY)).hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult post(Jwt jwt, String body) {
        return mvc.post()
                .uri(URI)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .exchange();
    }
}
