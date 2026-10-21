package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.CreateShipmentCommand;
import com.yarnshipment.shipment.application.port.in.CreateShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.CreatedShipment;
import com.yarnshipment.shipment.domain.model.InvalidNewShipmentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

/** Sevkiyat oluşturma ucunun sözleşmesi: izin, istek doğrulama ve hata eşlemesi. */
@WebMvcTest(controllers = ShipmentCreationController.class)
class CreateShipmentControllerTest {

    private static final String URI = "/api/v1/shipments";
    private static final String KEY = "0192f0a0-0000-7000-8000-0000000000dd";
    private static final String VALID_BODY = "{\"siteId\": \"" + TestTokens.SITE_ID + "\", \"subcontractorId\": \""
            + TestTokens.SUBCONTRACTOR_ID + "\", \"subcontractorTitle\": \"Örnek Tekstil\"}";

    private final MockMvcTester mvc;

    @MockitoBean
    private CreateShipmentUseCase createShipmentUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    CreateShipmentControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
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

    @Test
    @DisplayName("Depo çalışanı taslak oluşturur: 201 döner ve numara gövdede gelir")
    void shouldCreateShipment() {
        when(createShipmentUseCase.createShipment(any()))
                .thenReturn(new CreatedShipment(TestShipments.SHIPMENT_ID, "SV-20261021-ABCD1234"));

        MvcTestResult result = post(TestTokens.token("warehouse").build(), VALID_BODY);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.shipmentNo").isEqualTo("SV-20261021-ABCD1234");
        verify(createShipmentUseCase)
                .createShipment(argThat(
                        (CreateShipmentCommand command) -> command.siteId().equals(TestTokens.SITE_ID)
                                && "Örnek Tekstil".equals(command.parties().subcontractorTitle())));
    }

    @Test
    @DisplayName("Oluşturma izni olmayan kullanıcı 403 alır")
    void shouldRejectUserWithoutCreatePermission() {
        MvcTestResult result = post(TestTokens.subcontractorToken(), VALID_BODY);

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(createShipmentUseCase);
    }

    @Test
    @DisplayName("Eksik zorunlu alan 400 alır")
    void shouldRejectMissingField() {
        MvcTestResult result =
                post(TestTokens.token("warehouse").build(), "{\"siteId\": \"" + TestTokens.SITE_ID + "\"}");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(createShipmentUseCase);
    }

    @Test
    @DisplayName("Tutarsız şoför bilgisi 400 INVALID_SHIPMENT döner")
    void shouldMapInvalidShipment() {
        when(createShipmentUseCase.createShipment(any()))
                .thenThrow(new InvalidNewShipmentException("driverId", "unpaired"));

        MvcTestResult result = post(TestTokens.token("warehouse").build(), VALID_BODY);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_SHIPMENT");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("driverId");
    }
}
