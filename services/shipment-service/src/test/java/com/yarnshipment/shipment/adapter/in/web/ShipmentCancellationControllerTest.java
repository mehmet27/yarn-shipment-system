package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.CancelShipmentCommand;
import com.yarnshipment.shipment.application.port.in.CancelShipmentUseCase;
import com.yarnshipment.shipment.domain.bag.BagServiceUnavailableException;
import com.yarnshipment.shipment.domain.cancellation.InvalidCancellationException;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
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

/** Sevkiyat iptal ucunun sözleşmesi: izin, istek doğrulama ve hata eşlemesi. */
@WebMvcTest(controllers = ShipmentCancellationController.class)
class ShipmentCancellationControllerTest {

    private static final UUID SHIPMENT = UUID.fromString("0192f0a0-0000-7000-8000-000000000201");
    private static final String URI = "/api/v1/shipments/" + SHIPMENT + "/cancel";
    private static final String KEY = "0192f0a0-0000-7000-8000-0000000000dd";
    private static final String BODY = "{\"reason\": \"Yanlış sevkiyat\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private CancelShipmentUseCase cancelShipmentUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ShipmentCancellationControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Yönetici iptal eder; 204 döner ve gerekçe use case'e iletilir")
    void shouldCancel() throws Exception {
        MvcTestResult result = post(TestTokens.token("admin").build(), BODY);

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        ArgumentCaptor<CancelShipmentCommand> command = ArgumentCaptor.forClass(CancelShipmentCommand.class);
        verify(cancelShipmentUseCase).cancelShipment(command.capture());
        assertThat(command.getValue().reason()).isEqualTo("Yanlış sevkiyat");
        assertThat(command.getValue().shipmentId()).isEqualTo(SHIPMENT);
    }

    @Test
    @DisplayName("İptal izni olmayan rol 403 alır ve use case çağrılmaz")
    void shouldForbidWarehouse() throws Exception {
        MvcTestResult result = post(TestTokens.token("warehouse").build(), BODY);

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(cancelShipmentUseCase);
    }

    @Test
    @DisplayName("Boş gerekçe şemadan 400 alır")
    void shouldRejectEmptyReason() throws Exception {
        assertThat(post(TestTokens.token("admin").build(), "{\"reason\": \"\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(cancelShipmentUseCase);
    }

    @Test
    @DisplayName("Alan ve durum hataları kodlarıyla eşlenir: 400, 409 ve 502")
    void shouldMapErrors() throws Exception {
        Jwt admin = TestTokens.token("admin").build();
        doThrow(new InvalidCancellationException("reason"))
                .when(cancelShipmentUseCase)
                .cancelShipment(any());
        MvcTestResult invalid = post(admin, BODY);
        doThrow(new InvalidShipmentStatusException(SHIPMENT, ShipmentStatus.IN_TRANSIT, ShipmentStatus.CANCELLED))
                .when(cancelShipmentUseCase)
                .cancelShipment(any());
        MvcTestResult conflict = post(admin, BODY);
        doThrow(new BagServiceUnavailableException("down"))
                .when(cancelShipmentUseCase)
                .cancelShipment(any());
        MvcTestResult unavailable = post(admin, BODY);

        assertThat(invalid).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(invalid).bodyJson().extractingPath("$.code").isEqualTo("INVALID_CANCELLATION");
        assertThat(conflict).hasStatus(HttpStatus.CONFLICT);
        assertThat(unavailable).hasStatus(HttpStatus.BAD_GATEWAY);
    }

    private MvcTestResult post(Jwt jwt, String body) {
        return mvc.post()
                .uri(URI)
                .header("Idempotency-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .with(SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt)))
                .exchange();
    }
}
