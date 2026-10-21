package com.yarnshipment.shipment.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.yarnshipment.shipment.TestShipments;
import com.yarnshipment.shipment.TestTokens;
import com.yarnshipment.shipment.application.port.in.ConfirmPickupCommand;
import com.yarnshipment.shipment.application.port.in.ConfirmPickupUseCase;
import com.yarnshipment.shipment.application.port.in.GetShipmentUseCase;
import com.yarnshipment.shipment.application.port.in.ListShipmentsUseCase;
import com.yarnshipment.shipment.application.port.in.RecordDispatchUseCase;
import com.yarnshipment.shipment.application.port.in.SubmitDeliveryReceiptUseCase;
import com.yarnshipment.shipment.domain.model.InvalidShipmentStatusException;
import com.yarnshipment.shipment.domain.model.ShipmentStatus;
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

/** Durum geçişi uçlarının sözleşmesi: izin, zorunlu Idempotency-Key ve 409 eşlemesi. */
@WebMvcTest(controllers = ShipmentController.class)
class ShipmentTransitionControllerTest {

    private static final String PICKUP_URI = "/api/v1/shipments/" + TestShipments.SHIPMENT_ID + "/pickup-confirmation";
    private static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    private static final UUID KEY = UUID.fromString("0192f0a0-0000-7000-8000-0000000000cc");

    private final MockMvcTester mvc;

    @MockitoBean
    private ListShipmentsUseCase listShipmentsUseCase;

    @MockitoBean
    private GetShipmentUseCase getShipmentUseCase;

    @MockitoBean
    private ConfirmPickupUseCase confirmPickupUseCase;

    @MockitoBean
    private RecordDispatchUseCase recordDispatchUseCase;

    @MockitoBean
    private SubmitDeliveryReceiptUseCase submitDeliveryReceiptUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    ShipmentTransitionControllerTest(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    @Test
    @DisplayName("Şoförün teslim alma beyanı 204 döner ve komut token'daki kullanıcıyla kurulur")
    void shouldConfirmPickupForDriver() {
        // when
        MvcTestResult result = postPickup(TestTokens.driverToken(), KEY.toString());

        // then
        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(confirmPickupUseCase)
                .confirmPickup(org.mockito.ArgumentMatchers.argThat((ConfirmPickupCommand command) -> KEY.equals(
                                command.idempotencyKey())
                        && TestShipments.DRIVER_ID.equals(command.actor().driverId())));
    }

    @Test
    @DisplayName("Şoför olmayan kullanıcı teslim alma beyanı yapamaz (403)")
    void shouldRejectPickupFromWarehouse() {
        // when
        MvcTestResult result = postPickup(TestTokens.token("warehouse").build(), KEY.toString());

        // then
        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(confirmPickupUseCase);
    }

    @Test
    @DisplayName("Idempotency-Key başlığı olmayan istek 400 MISSING_HEADER alır")
    void shouldRequireIdempotencyKey() {
        // when
        MvcTestResult result = mvc.post()
                .uri(PICKUP_URI)
                .with(authenticated(TestTokens.driverToken()))
                .exchange();

        // then
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("MISSING_HEADER");
        verifyNoInteractions(confirmPickupUseCase);
    }

    @Test
    @DisplayName("Durum geçişe izin vermiyorsa 409 INVALID_SHIPMENT_STATUS döner")
    void shouldMapInvalidStatusToConflict() {
        // given
        doThrow(new InvalidShipmentStatusException(
                        TestShipments.SHIPMENT_ID, ShipmentStatus.DRAFT, ShipmentStatus.IN_TRANSIT))
                .when(confirmPickupUseCase)
                .confirmPickup(any());

        // when
        MvcTestResult result = postPickup(TestTokens.driverToken(), KEY.toString());

        // then
        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("INVALID_SHIPMENT_STATUS");
    }

    private MvcTestResult postPickup(Jwt jwt, String key) {
        return mvc.post()
                .uri(PICKUP_URI)
                .header(IDEMPOTENCY_KEY, key)
                .with(authenticated(jwt))
                .exchange();
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor authenticated(Jwt jwt) {
        return SecurityMockMvcRequestPostProcessors.authentication(TestTokens.CONVERTER.convert(jwt));
    }
}
