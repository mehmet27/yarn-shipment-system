package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.driver.DeclareArrivalCommand;
import com.yarnshipment.shipment.application.port.in.driver.DeclareArrivalUseCase;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
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

/** Varış beyanı ucunun sözleşmesi: izin, zorunlu alanlar ve 409 eşlemesi. */
@WebMvcTest(controllers = ShipmentArrivalController.class)
class ShipmentArrivalControllerTest {

    private static final String URI = "/api/v1/shipments/" + TestShipments.SHIPMENT_ID + "/arrival-declaration";
    private static final String KEY = "0192f0a0-0000-7000-8000-0000000000cd";
    private static final String BODY = "{\"sealIntact\": false}";

    private final MockMvcTester mvc;

    @MockitoBean
    private DeclareArrivalUseCase declareArrivalUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ShipmentArrivalControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Şoförün varış beyanı 204 döner; mühür durumu ve şoför kimliği komuta geçer")
    void shouldDeclareArrivalForDriver() {
        // when
        MvcTestResult result = post(TestTokens.driverToken(), BODY);

        // then
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        ArgumentCaptor<DeclareArrivalCommand> command = ArgumentCaptor.forClass(DeclareArrivalCommand.class);
        verify(declareArrivalUseCase).declareArrival(command.capture());
        assertThat(command.getValue().sealIntact()).isFalse();
        assertThat(command.getValue().actor().driverId()).isEqualTo(TestShipments.DRIVER_ID);
    }

    @Test
    @DisplayName("Şoför olmayan kullanıcı varış beyanı yapamaz (403)")
    void shouldRejectWarehouse() {
        // when
        MvcTestResult result = post(TestTokens.token("warehouse").build(), BODY);

        // then
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(declareArrivalUseCase);
    }

    @Test
    @DisplayName("Mühür durumu olmayan gövde 400 alır")
    void shouldRequireSealIntact() {
        // when
        MvcTestResult result = post(TestTokens.driverToken(), "{}");

        // then
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(declareArrivalUseCase);
    }

    @Test
    @DisplayName("Durum geçişe izin vermiyorsa 409 INVALID_SHIPMENT_STATUS döner")
    void shouldMapInvalidStatusToConflict() {
        // given
        doThrow(new InvalidShipmentStatusException(
                        TestShipments.SHIPMENT_ID, ShipmentStatus.LOADED, ShipmentStatus.AWAITING_DELIVERY))
                .when(declareArrivalUseCase)
                .declareArrival(any());

        // when
        MvcTestResult result = post(TestTokens.driverToken(), BODY);

        // then
        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_SHIPMENT_STATUS");
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
